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
import net.kemitix.s3thorp.awssdk.S3Client
import net.kemitix.s3thorp.domain.{Config, MD5Hash}

class Sync(s3Client: S3Client, md5HashGenerator: File => MD5Hash) {

  def run(info: Int => String => Unit,
          warn: String => Unit,
          error: String => Unit)
         (implicit c: Config): IO[Unit] = {
    logRunStart(info)
    s3Client.listObjects(c.bucket, c.prefix)
      .map { implicit s3ObjectsData => {
        logFileScan(info)
        val actions = for {
          file <- findFiles(c.source, md5HashGenerator, info)
          data <- getMetadata(file)
          action <- createActions(data)
          s3Action <- submitAction(s3Client, action)
        } yield s3Action
        val sorted = sort(actions.sequence)
        val list = sorted.unsafeRunSync.toList
        val delActions = (for {
          key <- s3ObjectsData.byKey.keys
          if key.isMissingLocally(c.source, c.prefix)
          ioDelAction <- submitAction(s3Client, ToDelete(key))
        } yield ioDelAction).toStream.sequence
        val delList = delActions.unsafeRunSync.toList
        logRunFinished(list ++ delList, info)
      }}
  }

  private def sort(ioActions: IO[Stream[S3Action]]) =
    ioActions.flatMap { actions => IO { actions.sorted } }

}
