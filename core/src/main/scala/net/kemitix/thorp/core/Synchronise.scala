package net.kemitix.thorp.core

import cats.data.NonEmptyChain
import cats.effect.IO
import cats.implicits._
import net.kemitix.thorp.core.Action.DoNothing
import net.kemitix.thorp.domain.{Config, LocalFile, Logger, RemoteKey, S3ObjectsData}
import net.kemitix.thorp.storage.api.StorageService

trait Synchronise {

  def apply(storageService: StorageService,
            configOptions: Seq[ConfigOption])
           (implicit logger: Logger): IO[Either[List[String], Stream[Action]]] =
    ConfigurationBuilder.buildConfig(configOptions)
      .flatMap {
        case Left(errors) => IO.pure(Left(errorMessages(errors)))
        case Right(config) => useValidConfig(storageService, config)
      }

  def errorMessages(errors: NonEmptyChain[ConfigValidation]): List[String] =
    errors.map(cv => cv.errorMessage).toList

  def useValidConfig(storageService: StorageService,
                     config: Config)
                    (implicit logger: Logger): IO[Either[List[String], Stream[Action]]] =
    gatherMetadata(storageService, logger, config)
      .map { md =>
        val (rd, ld) = md
        val actions1 = actionsForLocalFiles(config, ld, rd)
        val actions2 = actionsForRemoteKeys(config, rd)
        Right(actions1 ++ actions2)
      }

  private def gatherMetadata(storageService: StorageService,
                             logger: Logger,
                             config: Config) =
    for {
      remoteData <- fetchRemoteData(storageService, logger, config)
      localData <- findLocalFiles(config, logger)
    } yield (remoteData, localData)

  private def actionsForLocalFiles(config: Config, localData: Stream[LocalFile], remoteData: S3ObjectsData) =
    localData.foldLeft(Stream[Action]())((acc, lf) => createActionFromLocalFile(config, lf, remoteData) ++ acc)

  private def actionsForRemoteKeys(config: Config, remoteData: S3ObjectsData) =
    remoteData.byKey.keys.foldLeft(Stream[Action]())((acc, rk) => createActionFromRemoteKey(config, rk) #:: acc)

  private def fetchRemoteData(storageService: StorageService, logger: Logger, config: Config) =
    storageService.listObjects(config.bucket, config.prefix)(logger)

  private def findLocalFiles(implicit config: Config, l: Logger) =
    LocalFileStream.findFiles(config.source, MD5HashGenerator.md5File(_))

  private def createActionFromLocalFile(c: Config, lf: LocalFile, remoteData: S3ObjectsData) =
    ActionGenerator.createActions(S3MetaDataEnricher.getMetadata(lf, remoteData)(c))(c)

  private def createActionFromRemoteKey(c: Config, rk: RemoteKey) =
    if (rk.isMissingLocally(c.source, c.prefix)) Action.ToDelete(c.bucket, rk)
    else DoNothing(c.bucket, rk)

}

object Synchronise extends Synchronise
