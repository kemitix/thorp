package net.kemitix.thorp.core

import cats.effect.IO
import cats.implicits._
import net.kemitix.thorp.core.Action.DoNothing
import net.kemitix.thorp.domain.{Config, LocalFile, Logger, RemoteKey, S3ObjectsData}
import net.kemitix.thorp.storage.api.StorageService

trait Synchronise {

  def errorMessages(errors: List[ConfigValidation]): List[String] = {
    for {
      errorMessages <- errors.map(cv => cv.errorMessage)
    } yield errorMessages
  }

  def useValidConfig(storageService: StorageService,
                     logger: Logger,
                     config: Config): IO[Either[List[String], Stream[Action]]] =
    gatherMetadata(storageService, logger, config)
      .map { md => 
        val (rd, ld) = md
        val actions1 = actionsForLocalFiles(config, ld)
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

  private def actionsForRemoteKeys(config: Config, remoteData: S3ObjectsData) =
    remoteData.byKey.keys.foldLeft(Stream[Action]())((acc, rk) => createActionFromRemoteKey(config, rk) #:: acc)

  private def actionsForLocalFiles(config: Config, localData: Stream[LocalFile]) =
    localData.foldLeft(Stream[Action]())((acc, lf) => createActionFromLocalFile(config, lf) #:: acc)

  private def findLocalFiles(implicit config: Config, l: Logger) =
    LocalFileStream.findFiles(config.source, MD5HashGenerator.md5File(_))

  private def fetchRemoteData(storageService: StorageService, logger: Logger, config: Config) =
    storageService.listObjects(config.bucket, config.prefix)(logger)

  private def createActionFromRemoteKey(c: Config, rk: RemoteKey) =
    DoNothing(c.bucket, rk)

  private def createActionFromLocalFile(c: Config, lf: LocalFile) =
    DoNothing(c.bucket, lf.remoteKey)

  def apply(storageService: StorageService,
            configOptions: Seq[ConfigOption])
           (implicit logger: Logger): IO[Either[List[String], Stream[Action]]] =
    ConfigurationBuilder.buildConfig(configOptions)
      .flatMap {
        case Right(config) => useValidConfig(storageService, logger, config)
        case Left(errors) => IO.pure(Left(errorMessages(errors.toList)))
      }

}

object Synchronise extends Synchronise
