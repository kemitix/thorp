package net.kemitix.thorp.core

import java.io.File

import cats.effect.IO
import cats.implicits._
import net.kemitix.thorp.domain.{Bucket, Config, Logger, RemoteKey, StorageQueueEvent}
import net.kemitix.thorp.domain.StorageQueueEvent.{CopyQueueEvent, DeleteQueueEvent, ErrorQueueEvent, UploadQueueEvent}

trait SyncLogging {

  def logRunStart(bucket: Bucket,
                  prefix: RemoteKey,
                  source: File)
                 (implicit logger: Logger): IO[Unit] =
    logger.info(s"Bucket: ${bucket.name}, Prefix: ${prefix.key}, Source: $source, ")

  def logFileScan(implicit c: Config,
                  logger: Logger): IO[Unit] =
    logger.info(s"Scanning local files: ${c.source}...")

  def logErrors(actions: Stream[StorageQueueEvent])
               (implicit logger: Logger): IO[Unit] =
    for {
      _ <- actions.map {
        case ErrorQueueEvent(k, e) => logger.warn(s"${k.key}: ${e.getMessage}")
        case _ => IO.unit
      }.sequence
    } yield ()

  def logRunFinished(actions: Stream[StorageQueueEvent])
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
                              logger: Logger): (Counters, StorageQueueEvent) => Counters =
    (counters: Counters, s3Action: StorageQueueEvent) => {
      s3Action match {
        case _: UploadQueueEvent =>
          counters.copy(uploaded = counters.uploaded + 1)
        case _: CopyQueueEvent =>
          counters.copy(copied = counters.copied + 1)
        case _: DeleteQueueEvent =>
          counters.copy(deleted = counters.deleted + 1)
        case ErrorQueueEvent(k, e) =>
            counters.copy(errors = counters.errors + 1)
        case _ => counters
      }
    }

}

object SyncLogging extends SyncLogging
