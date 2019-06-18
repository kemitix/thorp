package net.kemitix.thorp.core

import cats.Monad
import cats.implicits._
import net.kemitix.thorp.aws.api.{S3Action, S3Client}
import net.kemitix.thorp.core.Action.ToDelete
import net.kemitix.thorp.core.ActionGenerator.createActions
import net.kemitix.thorp.core.ActionSubmitter.submitAction
import net.kemitix.thorp.core.LocalFileStream.findFiles
import net.kemitix.thorp.core.S3MetaDataEnricher.getMetadata
import net.kemitix.thorp.core.SyncLogging.{logFileScan, logRunFinished, logRunStart}
import net.kemitix.thorp.domain._

trait Sync {

  def errorMessages(errors: List[ConfigValidation]): List[String] = {
    for {
      errorMessages <- errors.map(cv => cv.errorMessage)
    } yield errorMessages
  }

  def apply[M[_]: Monad](s3Client: S3Client[M])
                        (configOptions: Seq[ConfigOption])
                        (implicit defaultLogger: Logger[M]): M[Either[List[String], Unit]] =
    ConfigurationBuilder(configOptions) match {
      case Left(errors) => Monad[M].pure(Left(errorMessages(errors.toList)))
      case Right(config) =>
        for {
          _ <- Sync.run[M](config, s3Client, defaultLogger.withDebug(config.debug))
        } yield Right(())
    }

  def run[M[_]: Monad](cliConfig: Config,
                       s3Client: S3Client[M],
                       logger: Logger[M]): M[Unit] = {

    implicit val c: Config = cliConfig
    implicit val l: Logger[M] = logger

    def metaData(s3Data: S3ObjectsData, sFiles: Stream[LocalFile]) =
      Monad[M].pure(sFiles.map(file => getMetadata(file, s3Data)))

    def actions(sData: Stream[S3MetaData]) =
      Monad[M].pure(sData.flatMap(s3MetaData => createActions(s3MetaData)))

    def submit(sActions: Stream[Action]) =
      Monad[M].pure(sActions.flatMap(action => submitAction[M](s3Client, action)))

    def copyUploadActions(s3Data: S3ObjectsData): M[Stream[S3Action]] =
      (for {
        files <- findFiles(c.source, MD5HashGenerator.md5File[M](_))
        metaData <- metaData(s3Data, files)
        actions <- actions(metaData)
        s3Actions <- submit(actions)
      } yield s3Actions.sequence)
        .flatten
        .map(streamS3Actions => streamS3Actions.sorted)

    def deleteActions(s3ObjectsData: S3ObjectsData): M[Stream[S3Action]] =
      (for {
        key <- s3ObjectsData.byKey.keys
        if key.isMissingLocally(c.source, c.prefix)
        ioDelAction <- submitAction[M](s3Client, ToDelete(c.bucket, key))
      } yield ioDelAction)
        .toStream
        .sequence

    for {
      _ <- logRunStart[M]
      s3data <- s3Client.listObjects(c.bucket, c.prefix)
      _ <- logFileScan[M]
      copyUploadActions <- copyUploadActions(s3data)
      deleteActions <- deleteActions(s3data)
      _ <- logRunFinished[M](copyUploadActions ++ deleteActions)
    } yield ()
  }
}

object Sync extends Sync
