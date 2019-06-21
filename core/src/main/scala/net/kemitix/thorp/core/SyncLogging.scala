package net.kemitix.thorp.core

import cats.effect.IO
import cats.implicits._
import net.kemitix.thorp.aws.api.S3Action
import net.kemitix.thorp.aws.api.S3Action.{CopyS3Action, DeleteS3Action, ErroredS3Action, UploadS3Action}
import net.kemitix.thorp.domain.{Config, Logger}

// Logging for the Sync class
object SyncLogging {

  def logRunStart(implicit c: Config,
                  logger: Logger): IO[Unit] =
    logger.info(s"Bucket: ${c.bucket.name}, Prefix: ${c.prefix.key}, Source: ${c.source}, ")

  def logFileScan(implicit c: Config,
                  logger: Logger): IO[Unit] =
    logger.info(s"Scanning local files: ${c.source}...")

  def logErrors(actions: Stream[S3Action])
               (implicit logger: Logger): IO[Unit] =
    for {
      _ <- actions.map {
        case ErroredS3Action(k, e) => logger.warn(s"${k.key}: ${e.getMessage}")
        case _ => IO.unit
      }.sequence
    } yield ()

  def logRunFinished(actions: Stream[S3Action])
                    (implicit c: Config,
                     logger: Logger): IO[Unit] = {
    val counters = actions.foldLeft(Counters())(countActivities)
    for {
      _ <- logger.info(s"Uploaded ${counters.uploaded} files")
      _ <- logger.info(s"Copied   ${counters.copied} files")
      _ <- logger.info(s"Deleted  ${counters.deleted} files")
      _ <- logger.info(s"Errors   ${counters.errors}")
      _ <- logErrors(actions)
    } yield ()
  }

  private def countActivities(implicit c: Config,
                              logger: Logger): (Counters, S3Action) => Counters =
    (counters: Counters, s3Action: S3Action) => {
      s3Action match {
        case _: UploadS3Action =>
          counters.copy(uploaded = counters.uploaded + 1)
        case _: CopyS3Action =>
          counters.copy(copied = counters.copied + 1)
        case _: DeleteS3Action =>
          counters.copy(deleted = counters.deleted + 1)
        case ErroredS3Action(k, e) =>
            counters.copy(errors = counters.errors + 1)
        case _ => counters
      }
    }

}
