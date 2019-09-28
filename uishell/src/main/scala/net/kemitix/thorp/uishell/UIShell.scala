package net.kemitix.thorp.uishell

import net.kemitix.eip.zio.MessageChannel
import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console.ConsoleOut.{
  CopyComplete,
  DeleteComplete,
  ErrorQueueEventOccurred,
  UploadComplete
}
import net.kemitix.thorp.console.{Console, ConsoleOut}
import net.kemitix.thorp.domain.Action.ToUpload
import net.kemitix.thorp.domain.Terminal.{eraseLineForward, eraseToEndOfScreen}
import net.kemitix.thorp.domain._
import zio.{UIO, ZIO}

object UIShell {

  def receiver: UIO[MessageChannel.UReceiver[Console with Config, UIEvent]] =
    UIO { uiEventMessage =>
      uiEventMessage.body match {
        case UIEvent.ShowValidConfig         => showValidConfig
        case UIEvent.RemoteDataFetched(size) => remoteDataFetched(size)
        case UIEvent.ShowSummary(counters)   => showSummary(counters)
        case UIEvent.FileFound(localFile)    => fileFound(localFile)
        case UIEvent.ActionChosen(action)    => actionChosen(action)
        case UIEvent.AwaitingAnotherUpload(remoteKey, hash) =>
          awaitingUpload(remoteKey, hash)
        case UIEvent.AnotherUploadWaitComplete(action) =>
          uploadWaitComplete(action)
        case UIEvent.ActionFinished(_, _, _, event) =>
          actionFinished(event)
        case UIEvent.KeyFound(_) => UIO(())
        case UIEvent.RequestCycle(localFile,
                                  bytesTransferred,
                                  index,
                                  totalBytesSoFar) =>
          ProgressUI.requestCycle(localFile,
                                  bytesTransferred,
                                  index,
                                  totalBytesSoFar)
      }
    }

  private def actionFinished(
      event: StorageEvent): ZIO[Console with Config, Nothing, Unit] =
    for {
      batchMode <- Config.batchMode
      _ <- event match {
        case StorageEvent.DoNothingEvent(remoteKey) => UIO.unit
        case StorageEvent.CopyEvent(sourceKey, targetKey) =>
          Console.putMessageLnB(CopyComplete(sourceKey, targetKey), batchMode)
        case StorageEvent.UploadEvent(remoteKey, md5Hash) =>
          Console.putMessageLnB(UploadComplete(remoteKey), batchMode)
        case StorageEvent.DeleteEvent(remoteKey) =>
          Console.putMessageLnB(DeleteComplete(remoteKey), batchMode)
        case StorageEvent.ErrorEvent(action, remoteKey, e) =>
          ProgressUI.finishedUploading(remoteKey) *>
            Console.putMessageLnB(ErrorQueueEventOccurred(action, e), batchMode)
        case StorageEvent.ShutdownEvent() => UIO.unit
      }
    } yield ()

  private def uploadWaitComplete(action: Action): ZIO[Console, Nothing, Unit] =
    Console.putStrLn(s"Finished waiting to other upload - now $action")

  private def awaitingUpload(remoteKey: RemoteKey,
                             hash: MD5Hash): ZIO[Console, Nothing, Unit] =
    Console.putStrLn(
      s"Awaiting another upload of $hash before copying it to $remoteKey")

  private def fileFound(
      localFile: LocalFile): ZIO[Console with Config, Nothing, Unit] =
    for {
      batchMode <- Config.batchMode
      _         <- ZIO.when(batchMode)(Console.putStrLn(s"Found: ${localFile.file}"))
    } yield ()

  private def showSummary(
      counters: Counters): ZIO[Console with Config, Nothing, Unit] =
    Console.putStrLn(eraseToEndOfScreen) *>
      Console.putStrLn(s"Uploaded ${counters.uploaded} files") *>
      Console.putStrLn(s"Copied   ${counters.copied} files") *>
      Console.putStrLn(s"Deleted  ${counters.deleted} files") *>
      Console.putStrLn(s"Errors   ${counters.errors}")

  private def remoteDataFetched(size: Int): ZIO[Console, Nothing, Unit] =
    Console.putStrLn(s"Found $size remote objects")

  private def showValidConfig: ZIO[Console with Config, Nothing, Unit] =
    for {
      bucket  <- Config.bucket
      prefix  <- Config.prefix
      sources <- Config.sources
      _       <- Console.putMessageLn(ConsoleOut.ValidConfig(bucket, prefix, sources))
    } yield ()

  private def actionAsString(action: Action): String = action match {
    case Action.DoNothing(bucket, remoteKey, size) =>
      s"Do nothing: ${remoteKey.key}"
    case ToUpload(bucket, localFile, size) =>
      s"Upload: ${localFile.remoteKey.key}"
    case Action.ToCopy(bucket, sourceKey, hash, targetKey, size) =>
      s"Copy: ${sourceKey.key} => ${targetKey.key}"
    case Action.ToDelete(bucket, remoteKey, size) => s"Delete: ${remoteKey.key}"
  }

  def trimHead(str: String): String = {
    val width = Terminal.width
    str.length match {
      case l if l > width => str.substring(l - width)
      case _              => str
    }
  }

  def actionChosen(action: Action): ZIO[Console with Config, Nothing, Unit] =
    for {
      batch <- Config.batchMode
      message = trimHead(actionAsString(action)) + eraseLineForward
      _ <- ZIO.when(!batch) { Console.putStr(message + "\r") }
      _ <- ZIO.when(batch) { Console.putStrLn(message) }
    } yield ()

}
