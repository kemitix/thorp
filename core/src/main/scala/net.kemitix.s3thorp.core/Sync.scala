package net.kemitix.s3thorp.core

import java.io.File

import cats.effect.IO
import cats.implicits._
import net.kemitix.s3thorp.aws.api.{S3Action, S3Client}
import net.kemitix.s3thorp.core.Action.ToDelete
import net.kemitix.s3thorp.core.ActionGenerator.createActions
import net.kemitix.s3thorp.core.ActionSubmitter.submitAction
import net.kemitix.s3thorp.core.LocalFileStream.findFiles
import net.kemitix.s3thorp.core.S3MetaDataEnricher.getMetadata
import net.kemitix.s3thorp.core.SyncLogging.{logFileScan, logRunFinished, logRunStart}
import net.kemitix.s3thorp.domain.{Config, LocalFile, MD5Hash, S3MetaData, S3ObjectsData}

object Sync {

  def run(s3Client: S3Client[IO],
          md5HashGenerator: File => IO[MD5Hash],
          info: Int => String => IO[Unit],
          warn: String => IO[Unit],
          error: String => IO[Unit])
         (implicit c: Config): IO[Unit] = {

    implicit val logInfo: Int => String => IO[Unit] = info
    implicit val logWarn: String => IO[Unit] = warn

    def metaData(s3Data: S3ObjectsData, sFiles: Stream[LocalFile]) =
      IO(sFiles.map(file => getMetadata(file, s3Data)))

    def actions(sData: Stream[S3MetaData]) =
      IO(sData.flatMap(s3MetaData => createActions(s3MetaData)))

    def submit(sActions: Stream[Action]) =
      IO(sActions.flatMap(action => submitAction[IO](s3Client, action)))

    def copyUploadActions(s3Data: S3ObjectsData): IO[Stream[S3Action]] =
      (for {
        files <- findFiles(c.source, md5HashGenerator, info)
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
        ioDelAction <- submitAction[IO](s3Client, ToDelete(c.bucket, key))
      } yield ioDelAction)
        .toStream
        .sequence

    for {
      _ <- logRunStart(info)
      s3data <- s3Client.listObjects(c.bucket, c.prefix)(info)
      _ <- logFileScan(info)
      copyUploadActions <- copyUploadActions(s3data)
      deleteActions <- deleteActions(s3data)
      _ <- logRunFinished(copyUploadActions ++ deleteActions, info)
    } yield ()
  }

}
