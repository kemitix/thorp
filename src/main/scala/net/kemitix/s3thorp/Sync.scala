package net.kemitix.s3thorp

import java.io.File

import cats.effect.IO
import cats.implicits._
import net.kemitix.s3thorp.Action.ToDelete
import net.kemitix.s3thorp.ActionGenerator.createActions
import net.kemitix.s3thorp.ActionSubmitter.submitAction
import net.kemitix.s3thorp.LocalFileStream.findFiles
import net.kemitix.s3thorp.S3MetaDataEnricher.getMetadata
import net.kemitix.s3thorp.SyncLogging.{logFileScan, logRunFinished, logRunStart}
import net.kemitix.s3thorp.aws.api.S3Action
import net.kemitix.s3thorp.aws.api.S3Client
import net.kemitix.s3thorp.domain.{Config, MD5Hash, S3ObjectsData}

object Sync {

  def run(s3Client: S3Client,
          md5HashGenerator: File => MD5Hash,
          info: Int => String => Unit,
          warn: String => Unit,
          error: String => Unit)
         (implicit c: Config): IO[Unit] = {
    def copyUploadActions(s3Data: S3ObjectsData) = {
      for {actions <- {
        for {
          file <- findFiles(c.source, md5HashGenerator, info)
          data <- getMetadata(file, s3Data)
          action <- createActions(data)
          s3Action <- submitAction(s3Client, action)(c, info, warn)
        } yield s3Action
      }.sequence
      } yield actions.sorted
    }

    def deleteActions(s3ObjectsData: S3ObjectsData) = {
      (for {
        key <- s3ObjectsData.byKey.keys
        if key.isMissingLocally(c.source, c.prefix)
        ioDelAction <- submitAction(s3Client, ToDelete(c.bucket, key))(c, info, warn)
      } yield ioDelAction).toStream.sequence
    }

    for {
      _ <- logRunStart(info)
      s3data <- s3Client.listObjects(c.bucket, c.prefix)(info)
      _ <- logFileScan(info)
      copyUploadActions <- copyUploadActions(s3data)
      deleteAction <- deleteActions(s3data)
      _ <- logRunFinished(copyUploadActions ++ deleteAction, info)
    } yield ()
  }

}
