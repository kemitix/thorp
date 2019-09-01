package net.kemitix.throp.uishell

import net.kemitix.eip.zio.MessageChannel
import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console.{Console, ConsoleOut}
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
      }
    }
}
