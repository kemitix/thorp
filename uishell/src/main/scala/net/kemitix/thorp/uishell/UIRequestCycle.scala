package net.kemitix.thorp.uishell

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console.Console
import net.kemitix.thorp.domain.SizeTranslation.sizeInEnglish
import net.kemitix.thorp.domain.Terminal.{
  eraseLineForward,
  eraseToEndOfScreen,
  progressBar
}
import net.kemitix.thorp.domain.{LocalFile, Terminal}
import zio.ZIO

import scala.io.AnsiColor.{GREEN, RESET}

object UIRequestCycle {
  def handle(localFile: LocalFile,
             bytesTransferred: Long,
             index: Int,
             totalBytesSoFar: Long): ZIO[Console with Config, Nothing, Unit] =
    ZIO.when(bytesTransferred < localFile.file.length()) {
      //TODO create and maintain some state and display that state
      val fileLength   = localFile.file.length
      val remoteKey    = localFile.remoteKey.key
      val statusHeight = 3
      val percent      = f"${(bytesTransferred * 100) / fileLength}%2d"
      Console.putStrLn(
        s"${GREEN}Uploading:$RESET $remoteKey$eraseToEndOfScreen\n" +
          s"$GREEN File:$RESET ($percent%) ${sizeInEnglish(bytesTransferred)} of ${sizeInEnglish(fileLength)}" +
          s"$eraseLineForward\n" +
          progressBar(bytesTransferred, fileLength, Terminal.width) +
          s"${Terminal.cursorPrevLine(statusHeight)}")
    }
}
