package net.kemitix.throp.uishell

import net.kemitix.eip.zio.MessageChannel
import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console.{Console, ConsoleOut}
import net.kemitix.thorp.domain.Terminal.eraseToEndOfScreen
import zio.UIO

object UIShell {
  def receiver: UIO[MessageChannel.UReceiver[Console with Config, UIEvent]] =
    UIO { uiEventMessage =>
      uiEventMessage.body match {
        case UIEvent.ShowValidConfig =>
          for {
            bucket  <- Config.bucket
            prefix  <- Config.prefix
            sources <- Config.sources
            _ <- Console.putMessageLn(
              ConsoleOut.ValidConfig(bucket, prefix, sources))
          } yield ()
        case UIEvent.RemoteDataFetched(size) =>
          Console.putStrLn(s"Found $size remote objects")
        case UIEvent.ShowSummary(counters) =>
          Console.putStrLn(eraseToEndOfScreen) *>
            Console.putStrLn(s"Uploaded ${counters.uploaded} files") *>
            Console.putStrLn(s"Copied   ${counters.copied} files") *>
            Console.putStrLn(s"Deleted  ${counters.deleted} files") *>
            Console.putStrLn(s"Errors   ${counters.errors}")
      }
    }
}
