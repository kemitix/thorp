package net.kemitix.thorp.core

import cats.data.NonEmptyChain
import cats.data.EitherT
import cats.effect.IO
import cats.implicits._
import net.kemitix.thorp.core.Action.DoNothing
import net.kemitix.thorp.domain.{Config, LocalFile, Logger, RemoteKey, S3ObjectsData}
import net.kemitix.thorp.storage.api.StorageService

trait Synchronise {

  def apply(storageService: StorageService,
            configOptions: Seq[ConfigOption])
           (implicit logger: Logger): EitherT[IO, List[String], Stream[Action]] =
    EitherT(ConfigurationBuilder.buildConfig(configOptions))
      .swap.map(errorMessages).swap
      .flatMap(config => useValidConfig(storageService, config))

  def errorMessages(errors: NonEmptyChain[ConfigValidation]): List[String] =
    errors.map(cv => cv.errorMessage).toList

  def removeDoNothing: Action => Boolean = {
    case _: DoNothing => false
    case _ => true
  }

  def useValidConfig(storageService: StorageService,
                     config: Config)
                    (implicit logger: Logger): EitherT[IO, List[String], Stream[Action]] = {
    for {
      _ <- EitherT.liftF(SyncLogging.logRunStart(config.bucket, config.prefix, config.source))
      actions <- gatherMetadata(storageService, logger, config)
        .swap.map(error => List(error)).swap
        .map {
          case (remoteData, localData) =>
            (actionsForLocalFiles(config, localData, remoteData) ++
              actionsForRemoteKeys(config, remoteData))
              .filter(removeDoNothing)
        }
    } yield actions
  }

  private def gatherMetadata(storageService: StorageService,
                             logger: Logger,
                             config: Config): EitherT[IO, String, (S3ObjectsData, Stream[LocalFile])] =
    for {
      remoteData <- fetchRemoteData(storageService, config)
      localData <- EitherT.liftF(findLocalFiles(config, logger))
    } yield (remoteData, localData)

  private def actionsForLocalFiles(config: Config, localData: Stream[LocalFile], remoteData: S3ObjectsData) =
    localData.foldLeft(Stream[Action]())((acc, lf) => createActionFromLocalFile(config, lf, remoteData) ++ acc)

  private def actionsForRemoteKeys(config: Config, remoteData: S3ObjectsData) =
    remoteData.byKey.keys.foldLeft(Stream[Action]())((acc, rk) => createActionFromRemoteKey(config, rk) #:: acc)

  private def fetchRemoteData(storageService: StorageService, config: Config) =
    storageService.listObjects(config.bucket, config.prefix)

  private def findLocalFiles(implicit config: Config, l: Logger) =
    LocalFileStream.findFiles(config.source, MD5HashGenerator.md5File(_))

  private def createActionFromLocalFile(c: Config, lf: LocalFile, remoteData: S3ObjectsData) =
    ActionGenerator.createActions(S3MetaDataEnricher.getMetadata(lf, remoteData)(c))(c)

  private def createActionFromRemoteKey(c: Config, rk: RemoteKey) =
    if (rk.isMissingLocally(c.source, c.prefix)) Action.ToDelete(c.bucket, rk)
    else DoNothing(c.bucket, rk)

}

object Synchronise extends Synchronise
