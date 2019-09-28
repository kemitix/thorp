package net.kemitix.thorp.uishell

import java.util.concurrent.atomic.AtomicReference

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console.Console
import net.kemitix.thorp.domain.SizeTranslation.sizeInEnglish
import net.kemitix.thorp.domain.Terminal.{eraseLineForward, progressBar}
import net.kemitix.thorp.domain.{LocalFile, RemoteKey, Terminal}
import zio.{UIO, ZIO}

import scala.io.AnsiColor.{GREEN, RESET}

object ProgressUI {

  private case class UploadState(transferred: Long, fileLength: Long)

  private val uploads: AtomicReference[Map[RemoteKey, UploadState]] =
    new AtomicReference[Map[RemoteKey, UploadState]](Map.empty)

  private val statusHeight = 3

  def handle(localFile: LocalFile,
             bytesTransferred: Long,
             index: Int,
             totalBytesSoFar: Long): ZIO[Console with Config, Nothing, Unit] =
    for {
      _ <- ZIO.when(bytesTransferred < localFile.file.length())(
        stillUploading(localFile.remoteKey,
                       localFile.file.length(),
                       bytesTransferred))
      _ <- ZIO.when(bytesTransferred >= localFile.file.length()) {
        finishedUploading(localFile.remoteKey)
      }
    } yield ()

  private def stillUploading(
      remoteKey: RemoteKey,
      fileLength: Long,
      bytesTransferred: Long
  ): ZIO[Console, Nothing, Unit] = {
    val current: Map[RemoteKey, UploadState] =
      uploads.updateAndGet((m: Map[RemoteKey, UploadState]) =>
        m.updated(remoteKey, UploadState(bytesTransferred, fileLength)))
    val resetCursor = s"${Terminal.cursorPrevLine(statusHeight) * current.size}"
    ZIO.foreach(current) { entry =>
      {
        val (remoteKey, state) = entry

        val percent     = f"${(state.transferred * 100) / state.fileLength}%2d"
        val transferred = sizeInEnglish(state.transferred)
        val fileLength  = sizeInEnglish(state.fileLength)
        val line1 =
          s"${GREEN}Uploading:$RESET ${remoteKey.key}$eraseLineForward"
        val line2 = s"$GREEN File:$RESET ($percent%) $transferred of $fileLength" + s"$eraseLineForward"
        val line3 =
          progressBar(state.transferred, state.fileLength, Terminal.width)
        Console.putStrLn(line1) *>
          Console.putStrLn(line2) *>
          Console.putStrLn(line3)
      }
    } *> Console.putStr(resetCursor)
  }

  private def finishedUploading(
      remoteKey: RemoteKey
  ): ZIO[Any, Nothing, Unit] = {
    UIO(uploads.updateAndGet((m: Map[RemoteKey, UploadState]) =>
      m.removed(remoteKey))) *> UIO.unit
  }

}
