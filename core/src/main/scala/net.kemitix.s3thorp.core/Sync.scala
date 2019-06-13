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
import net.kemitix.s3thorp.domain.{Config, MD5Hash, S3ObjectsData}

object Sync {

  def run(s3Client: S3Client[IO],
          md5HashGenerator: File => IO[MD5Hash],
          info: Int => String => IO[Unit],
          warn: String => IO[Unit],
          error: String => IO[Unit])
         (implicit c: Config): IO[Unit] = {

    def copyUploadActions(s3Data: S3ObjectsData): IO[Stream[S3Action]] =
      (for {
        sFiles <- findFiles(c.source, md5HashGenerator, info)
        sData <- IO(sFiles.map(file => getMetadata(file, s3Data)))
        sActions <- IO(sData.flatMap(s3MetaData => createActions(s3MetaData)))
        sS3Actions <- IO(sActions.flatMap(action => submitAction(s3Client, action)(c, info, warn)))
      } yield sS3Actions.sequence)
        .flatten
        .map(streamS3Actions => streamS3Actions.sorted)

    def deleteActions(s3ObjectsData: S3ObjectsData): IO[Stream[S3Action]] =
      (for {
        key <- s3ObjectsData.byKey.keys
        if key.isMissingLocally(c.source, c.prefix)
        ioDelAction <- submitAction(s3Client, ToDelete(c.bucket, key))(c, info, warn)
      } yield ioDelAction).toStream.sequence

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
