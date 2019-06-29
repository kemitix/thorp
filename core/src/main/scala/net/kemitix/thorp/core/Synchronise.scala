package net.kemitix.thorp.core

import cats.data.NonEmptyChain
import cats.data.EitherT
import cats.effect.IO
import cats.implicits._
import net.kemitix.thorp.core.Action.DoNothing
import net.kemitix.thorp.domain.{Config, LocalFile, Logger, RemoteKey, S3ObjectsData}
import net.kemitix.thorp.storage.api.{HashService, StorageService}

trait Synchronise {

  def apply(storageService: StorageService,
            hashService: HashService,
            configOptions: Seq[ConfigOption])
           (implicit l: Logger): EitherT[IO, List[String], Stream[Action]] =
    EitherT(ConfigurationBuilder.buildConfig(configOptions))
      .swap.map(errorMessages).swap
      .flatMap(config => useValidConfig(storageService, hashService)(config, l))

  def errorMessages(errors: NonEmptyChain[ConfigValidation]): List[String] =
    errors.map(cv => cv.errorMessage).toList

  def removeDoNothing: Action => Boolean = {
    case _: DoNothing => false
    case _ => true
  }

  def useValidConfig(storageService: StorageService,
                     hashService: HashService)
                    (implicit c: Config, l: Logger): EitherT[IO, List[String], Stream[Action]] = {
    for {
      _ <- EitherT.liftF(SyncLogging.logRunStart(c.bucket, c.prefix, c.source))
      actions <- gatherMetadata(storageService, hashService)
        .swap.map(error => List(error)).swap
        .map {
          case (remoteData, localData) =>
            (actionsForLocalFiles(localData, remoteData) ++
              actionsForRemoteKeys(remoteData))
              .filter(removeDoNothing)
        }
    } yield actions
  }

  private def gatherMetadata(storageService: StorageService,
                             hashService: HashService)
                            (implicit l: Logger,
                             c: Config): EitherT[IO, String, (S3ObjectsData, Stream[LocalFile])] =
    for {
      remoteData <- fetchRemoteData(storageService)
      localData <- EitherT.liftF(findLocalFiles(hashService))
    } yield (remoteData, localData)

  private def actionsForLocalFiles(localData: Stream[LocalFile], remoteData: S3ObjectsData)
                                  (implicit c: Config) =
    localData.foldLeft(Stream[Action]())((acc, lf) => createActionFromLocalFile(lf, remoteData) ++ acc)

  private def actionsForRemoteKeys(remoteData: S3ObjectsData)
                                  (implicit c: Config) =
    remoteData.byKey.keys.foldLeft(Stream[Action]())((acc, rk) => createActionFromRemoteKey(rk) #:: acc)

  private def fetchRemoteData(storageService: StorageService)(implicit c: Config) =
    storageService.listObjects(c.bucket, c.prefix)

  private def findLocalFiles(hashService: HashService)(implicit config: Config, l: Logger) =
    LocalFileStream.findFiles(config.source, hashService)

  private def createActionFromLocalFile(lf: LocalFile, remoteData: S3ObjectsData)
                                       (implicit c: Config) =
    ActionGenerator.createActions(S3MetaDataEnricher.getMetadata(lf, remoteData))

  private def createActionFromRemoteKey(rk: RemoteKey)
                                       (implicit c: Config) =
    if (rk.isMissingLocally(c.source, c.prefix)) Action.ToDelete(c.bucket, rk)
    else DoNothing(c.bucket, rk)

}

object Synchronise extends Synchronise
