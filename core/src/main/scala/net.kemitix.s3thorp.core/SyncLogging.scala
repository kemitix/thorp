package net.kemitix.s3thorp.core

import cats.Monad
import cats.implicits._
import net.kemitix.s3thorp.aws.api.S3Action
import net.kemitix.s3thorp.aws.api.S3Action.{CopyS3Action, DeleteS3Action, ErroredS3Action, UploadS3Action}
import net.kemitix.s3thorp.domain.{Config, Logger}

// Logging for the Sync class
object SyncLogging {

  def logRunStart[M[_]: Monad](implicit c: Config,
                               logger: Logger[M]): M[Unit] =
    logger.info(s"Bucket: ${c.bucket.name}, Prefix: ${c.prefix.key}, Source: ${c.source}, ")

  def logFileScan[M[_]: Monad](implicit c: Config,
                               logger: Logger[M]): M[Unit] =
    logger.info(s"Scanning local files: ${c.source}...")

  def logRunFinished[M[_]: Monad](actions: Stream[S3Action])
                                 (implicit c: Config,
                                  logger: Logger[M]): M[Unit] = {
    val counters = actions.foldLeft(Counters())(countActivities)
    for {
      _ <- logger.info(s"Uploaded ${counters.uploaded} files")
      _ <- logger.info(s"Copied   ${counters.copied} files")
      _ <- logger.info(s"Deleted  ${counters.deleted} files")
      _ <- logger.info(s"Errors   ${counters.errors}")
    } yield ()
  }

  private def countActivities(implicit c: Config): (Counters, S3Action) => Counters =
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
