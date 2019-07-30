package net.kemitix.thorp.core

import net.kemitix.thorp.config._
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
      bucket  <- getBucket
      prefix  <- getPrefix
      sources <- getSources
      _       <- Console.putMessageLn(ConsoleOut.ValidConfig(bucket, prefix, sources))
    } yield ()

  def logFileScan: ZIO[Config with Console, Nothing, Unit] =
    for {
      sources <- getSources
      _ <- Console.putStrLn(
        s"Scanning local files: ${sources.paths.mkString(", ")}...")
    } yield ()

  def logRunFinished(
      actions: Stream[StorageQueueEvent]
  ): ZIO[Console, Nothing, Unit] = {
    val counters = actions.foldLeft(Counters())(countActivities)
    for {
      _ <- Console.putStrLn(eraseToEndOfScreen)
      _ <- Console.putStrLn(s"Uploaded ${counters.uploaded} files")
      _ <- Console.putStrLn(s"Copied   ${counters.copied} files")
      _ <- Console.putStrLn(s"Deleted  ${counters.deleted} files")
      _ <- Console.putStrLn(s"Errors   ${counters.errors}")
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
