package net.kemitix.thorp.core

import cats.effect.IO
import cats.implicits._
import net.kemitix.thorp.domain.StorageQueueEvent.{
  CopyQueueEvent,
  DeleteQueueEvent,
  ErrorQueueEvent,
  UploadQueueEvent
}
import net.kemitix.thorp.domain._

trait SyncLogging {

  def logRunStart(
      bucket: Bucket,
      prefix: RemoteKey,
      sources: Sources
  )(implicit logger: Logger): IO[Unit] = {
    val sourcesList = sources.paths.mkString(", ")
    logger.info(s"Bucket: ${bucket.name}, Prefix: ${prefix.key}, Source: $sourcesList")
  }

  def logFileScan(implicit c: Config,
                  logger: Logger): IO[Unit] =
    logger.info(s"Scanning local files: ${c.sources.paths.mkString(", ")}...")

  def logRunFinished(
      actions: Stream[StorageQueueEvent]
  )(implicit logger: Logger): IO[Unit] = {
    val counters = actions.foldLeft(Counters())(countActivities)
    for {
      _ <- logger.info(s"Uploaded ${counters.uploaded} files")
      _ <- logger.info(s"Copied   ${counters.copied} files")
      _ <- logger.info(s"Deleted  ${counters.deleted} files")
      _ <- logger.info(s"Errors   ${counters.errors}")
      _ <- logErrors(actions)
    } yield ()
  }

  def logErrors(
      actions: Stream[StorageQueueEvent]
  )(implicit logger: Logger): IO[Unit] =
    for {
      _ <- actions.map {
        case ErrorQueueEvent(k, e) => logger.warn(s"${k.key}: ${e.getMessage}")
        case _                     => IO.unit
      }.sequence
    } yield ()

  private def countActivities: (Counters, StorageQueueEvent) => Counters =
    (counters: Counters, s3Action: StorageQueueEvent) => {
      import Counters._
      val increment: Int => Int = _ + 1
      s3Action match {
        case _: UploadQueueEvent => uploaded.modify(increment)(counters)
        case _: CopyQueueEvent   => copied.modify(increment)(counters)
        case _: DeleteQueueEvent => deleted.modify(increment)(counters)
        case _: ErrorQueueEvent  => errors.modify(increment)(counters)
        case _                   => counters
      }
    }

}

object SyncLogging extends SyncLogging
