package net.kemitix.thorp.core

import cats.effect.IO
import cats.implicits._
import net.kemitix.thorp.core.Action.ToDelete
import net.kemitix.thorp.core.ActionGenerator.createActions
import net.kemitix.thorp.core.ActionSubmitter.submitAction
import net.kemitix.thorp.core.ConfigurationBuilder.buildConfig
import net.kemitix.thorp.core.LocalFileStream.findFiles
import net.kemitix.thorp.core.S3MetaDataEnricher.getMetadata
import net.kemitix.thorp.core.SyncLogging.{logFileScan, logRunFinished, logRunStart}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.{S3Action, S3Client}

trait Sync {

  def errorMessages(errors: List[ConfigValidation]): List[String] = {
    for {
      errorMessages <- errors.map(cv => cv.errorMessage)
    } yield errorMessages
  }

  def apply(s3Client: S3Client)
           (configOptions: Seq[ConfigOption])
           (implicit defaultLogger: Logger): IO[Either[List[String], Unit]] =
    buildConfig(configOptions).flatMap {
      case Right(config) => runWithValidConfig(s3Client, defaultLogger, config)
      case Left(errors) => IO.pure(Left(errorMessages(errors.toList)))
    }

  private def runWithValidConfig(s3Client: S3Client,
                                 defaultLogger: Logger,
                                 config: Config) = {
    for {
      _ <- run(config, s3Client, defaultLogger.withDebug(config.debug))
    } yield Right(())
  }

  private def run(cliConfig: Config,
                  s3Client: S3Client,
                  logger: Logger): IO[Unit] = {

    implicit val c: Config = cliConfig
    implicit val l: Logger = logger

    def metaData(s3Data: S3ObjectsData, sFiles: Stream[LocalFile]) =
      IO.pure(sFiles.map(file => getMetadata(file, s3Data)))

    def actions(sData: Stream[S3MetaData]) =
      IO.pure(sData.flatMap(s3MetaData => createActions(s3MetaData)))

    def submit(sActions: Stream[Action]) =
      IO(sActions.flatMap(action => submitAction(s3Client, action)))

    def copyUploadActions(s3Data: S3ObjectsData): IO[Stream[S3Action]] =
      (for {
        files <- findFiles(c.source, MD5HashGenerator.md5File(_))
        metaData <- metaData(s3Data, files)
        actions <- actions(metaData)
        s3Actions <- submit(actions)
      } yield s3Actions.sequence)
        .flatten
        .map(streamS3Actions => streamS3Actions.sorted)

    def deleteActions(s3ObjectsData: S3ObjectsData): IO[Stream[S3Action]] =
      (for {
        key <- s3ObjectsData.byKey.keys
        if key.isMissingLocally(c.source, c.prefix)
        ioDelAction <- submitAction(s3Client, ToDelete(c.bucket, key))
      } yield ioDelAction)
        .toStream
        .sequence

    for {
      _ <- logRunStart
      s3data <- s3Client.listObjects(c.bucket, c.prefix)
      _ <- logFileScan
      copyUploadActions <- copyUploadActions(s3data)
      deleteActions <- deleteActions(s3data)
      _ <- logRunFinished(copyUploadActions ++ deleteActions)
    } yield ()
  }
}

object Sync extends Sync
