package net.kemitix.thorp.core

import cats.effect.IO
import cats.implicits._
import net.kemitix.thorp.core.Action.DoNothing
import net.kemitix.thorp.domain.{Config, LocalFile, Logger, RemoteKey}
import net.kemitix.thorp.storage.api.StorageService

trait Synchronise {

  def errorMessages(errors: List[ConfigValidation]): List[String] = {
    for {
      errorMessages <- errors.map(cv => cv.errorMessage)
    } yield errorMessages
  }

  def useValidConfig(storageService: StorageService,
                     logger: Logger,
                     config: Config): IO[Either[List[String], Stream[Action]]] = {
    implicit val l: Logger = logger
    implicit val c: Config = config

    storageService.listObjects(config.bucket, config.prefix)(logger)
      .flatMap { s3ObjectsData =>
        LocalFileStream.findFiles(config.source, MD5HashGenerator.md5File(_))
          .map { streamLocalFiles =>
            AppState.Initial().toConfigured(config).toScanLocalFiles(s3ObjectsData, streamLocalFiles)
          }
      }.map { scanLocalFiles =>
      scanLocalFiles.toScanRemoteKeys(
        scanLocalFiles.localData
          .foldLeft(Stream[Action]())((acc, lf) => createActionFromLocalFile(c, lf) #:: acc))
    }.map { scanRemoteKeys =>
      scanRemoteKeys.toCompleted(
        scanRemoteKeys.remoteData
          .foldLeft(Stream[Action]())((acc, rk) => createActionFromRemoteKey(c, rk) #:: acc))
    }.map(c => Right(c.actions))
  }

  private def createActionFromRemoteKey(c: Config, rk: RemoteKey) = {
    DoNothing(c.bucket, rk)
  }

  private def createActionFromLocalFile(c: Config, lf: LocalFile) = {
    DoNothing(c.bucket, lf.remoteKey)
  }

  def apply(storageService: StorageService,
            configOptions: Seq[ConfigOption])
           (implicit logger: Logger): IO[Either[List[String], Stream[Action]]] = {
    ConfigurationBuilder.buildConfig(configOptions)
      .flatMap {
        case Right(config) => useValidConfig(storageService, logger, config)
        case Left(errors) => IO.pure(Left(errorMessages(errors.toList)))
      }
  }

}

object Synchronise extends Synchronise
