package net.kemitix.thorp.uishell

import java.util.concurrent.atomic.AtomicReference

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console.Console
import net.kemitix.thorp.domain.SizeTranslation.sizeInEnglish
import net.kemitix.thorp.domain.Terminal.{
  eraseLineForward,
  eraseToEndOfScreen,
  progressBar
}
import net.kemitix.thorp.domain.{LocalFile, RemoteKey, Terminal}
import zio.{UIO, ZIO}

import scala.io.AnsiColor.{GREEN, RESET}

object UIRequestCycle {

  private case class UploadState(transferred: Long, fileLength: Long)

  private val uploads: AtomicReference[Map[RemoteKey, UploadState]] =
    new AtomicReference[Map[RemoteKey, UploadState]](Map.empty)

  def handle(localFile: LocalFile,
             bytesTransferred: Long,
             index: Int,
             totalBytesSoFar: Long): ZIO[Console with Config, Nothing, Unit] =
    ZIO.when(bytesTransferred < localFile.file.length()) {
      val statusHeight = 3
      val current: Map[RemoteKey, UploadState] =
        uploads.updateAndGet(
          (m: Map[RemoteKey, UploadState]) =>
            m.updated(localFile.remoteKey,
                      UploadState(bytesTransferred, localFile.file.length())))
      ZIO.foreach(current) { entry =>
        {
          val (remoteKey, state) = entry
          val percent            = f"${(state.transferred * 100) / state.fileLength}%2d"
          Console.putStrLn(
            s"${GREEN}Uploading:$RESET ${remoteKey.key}$eraseToEndOfScreen") *>
            Console.putStrLn(s"$GREEN File:$RESET ($percent%) ${sizeInEnglish(
              state.transferred)} of ${sizeInEnglish(state.fileLength)}" +
              s"$eraseLineForward") *> Console.putStrLn(
            progressBar(state.transferred, state.fileLength, Terminal.width))
        }
      } *> Console.putStr(
        s"${Terminal.cursorPrevLine(statusHeight) * current.size}")
    } *> ZIO.when(bytesTransferred >= localFile.file.length()) {
      UIO(uploads.updateAndGet((m: Map[RemoteKey, UploadState]) =>
        m.removed(localFile.remoteKey))) *> UIO.unit
    }
}
