package net.kemitix.throp.uishell

import net.kemitix.eip.zio.MessageChannel
import net.kemitix.thorp.console.Console
import zio.UIO

object UIShell {
  def receiver: UIO[MessageChannel.Receiver[Console, UIEvent]] =
    UIO { uiEventMessage =>
      uiEventMessage.body match {
       case UIEvent.Ping() => Console.putStrLn("UI Shell - ping")
      }
    }
}
