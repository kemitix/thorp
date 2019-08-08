package net.kemitix.thorp.core

import net.kemitix.thorp.config.Config
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

  def logRunStart: ZIO[Console with Config, Nothing, Unit] =
    for {
      bucket  <- Config.bucket
      prefix  <- Config.prefix
      sources <- Config.sources
      _       <- Console.putMessageLn(ConsoleOut.ValidConfig(bucket, prefix, sources))
    } yield ()

  def logFileScan: ZIO[Config with Console, Nothing, Unit] =
    for {
      sources <- Config.sources
      _ <- Console.putStrLn(
        s"Scanning local files: ${sources.paths.mkString(", ")}...")
    } yield ()

  def logRunFinished(
      actions: Seq[StorageQueueEvent]
  ): ZIO[Console, Nothing, Unit] = {
    val counters = actions.foldLeft(Counters.empty)(countActivities)
    Console.putStrLn(eraseToEndOfScreen) *>
      Console.putStrLn(s"Uploaded ${counters.uploaded} files") *>
      Console.putStrLn(s"Copied   ${counters.copied} files") *>
      Console.putStrLn(s"Deleted  ${counters.deleted} files") *>
      Console.putStrLn(s"Errors   ${counters.errors}")
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
