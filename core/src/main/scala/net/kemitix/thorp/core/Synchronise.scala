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

    val ioScanLocalFiles = for {
      s3ObjectsData <- storageService.listObjects(config.bucket, config.prefix)(logger)
      slf <- LocalFileStream.findFiles(config.source, MD5HashGenerator.md5File(_))
    } yield AppState.Initial().toConfigured(config).toScanLocalFiles(s3ObjectsData, slf)

    val ioScanRemoteKeys = for {
      scanLocalFiles <- ioScanLocalFiles
      actions = scanLocalFiles.localData
        .foldLeft(Stream[Action]())((acc, lf) => createActionFromLocalFile(c, lf) #:: acc)
      scanRemoteKeys = scanLocalFiles.toScanRemoteKeys(actions)
    } yield scanRemoteKeys

    val ioCompleted = for {
      scanRemoteKeys <- ioScanRemoteKeys
      actions = scanRemoteKeys.remoteData
        .foldLeft(Stream[Action]())((acc, rk) => createActionFromRemoteKey(c, rk) #:: acc)
      completed = scanRemoteKeys.toCompleted(actions)
    } yield completed

    for {
      complete <- ioCompleted
    } yield Right(complete.actions)


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
