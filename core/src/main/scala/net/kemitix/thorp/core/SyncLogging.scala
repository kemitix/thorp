package net.kemitix.thorp.core

import net.kemitix.thorp.console._
import net.kemitix.thorp.domain.StorageQueueEvent.{
  CopyQueueEvent,
  DeleteQueueEvent,
  ErrorQueueEvent,
  UploadQueueEvent
}
import net.kemitix.thorp.domain._
import net.kemitix.thorp.domain.Terminal.eraseToEndOfScreen
import zio.ZIO

trait SyncLogging {

  def logRunStart(
      bucket: Bucket,
      prefix: RemoteKey,
      sources: Sources
  ): ZIO[Console, Nothing, Unit] =
    for {
      _ <- putStrLn(ConsoleOut.ValidConfig(bucket, prefix, sources))
    } yield ()

  def logFileScan(implicit c: Config): ZIO[Console, Nothing, Unit] =
    putStrLn(s"Scanning local files: ${c.sources.paths.mkString(", ")}...")

  def logRunFinished(
      actions: Stream[StorageQueueEvent]
  ): ZIO[Console, Nothing, Unit] = {
    val counters = actions.foldLeft(Counters())(countActivities)
    for {
      _ <- putStrLn(eraseToEndOfScreen)
      _ <- putStrLn(s"Uploaded ${counters.uploaded} files")
      _ <- putStrLn(s"Copied   ${counters.copied} files")
      _ <- putStrLn(s"Deleted  ${counters.deleted} files")
      _ <- putStrLn(s"Errors   ${counters.errors}")
    } yield ()
  }

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
