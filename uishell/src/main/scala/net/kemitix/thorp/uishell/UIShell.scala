package net.kemitix.thorp.uishell

import net.kemitix.eip.zio.MessageChannel
import net.kemitix.thorp.config.Configuration
import net.kemitix.thorp.console.ConsoleOut.{
  CopyComplete,
  DeleteComplete,
  ErrorQueueEventOccurred,
  UploadComplete
}
import net.kemitix.thorp.console.{Console, ConsoleOut}
import net.kemitix.thorp.domain.Terminal.{eraseLineForward, eraseToEndOfScreen}
import net.kemitix.thorp.domain._
import zio.{UIO, ZIO}

object UIShell {

  def receiver(configuration: Configuration)
    : UIO[MessageChannel.UReceiver[Console, UIEvent]] =
    UIO { uiEventMessage =>
      uiEventMessage.body match {
        case UIEvent.ShowValidConfig         => showValidConfig(configuration)
        case UIEvent.RemoteDataFetched(size) => remoteDataFetched(size)
        case UIEvent.ShowSummary(counters)   => showSummary(counters)
        case UIEvent.FileFound(localFile)    => fileFound(configuration, localFile)
        case UIEvent.ActionChosen(action)    => actionChosen(configuration, action)
        case UIEvent.AwaitingAnotherUpload(remoteKey, hash) =>
          awaitingUpload(remoteKey, hash)
        case UIEvent.AnotherUploadWaitComplete(action) =>
          uploadWaitComplete(action)
        case UIEvent.ActionFinished(_, _, _, event) =>
          actionFinished(configuration, event)
        case UIEvent.KeyFound(_) => UIO(())
        case UIEvent.RequestCycle(localFile,
                                  bytesTransferred,
                                  index,
                                  totalBytesSoFar) =>
          ProgressUI.requestCycle(configuration,
                                  localFile,
                                  bytesTransferred,
                                  index,
                                  totalBytesSoFar)
      }
    }

  private def actionFinished(
      configuration: Configuration,
      event: StorageEvent): ZIO[Console, Nothing, Unit] = {
    val batchMode = configuration.batchMode
    for {
      _ <- event match {
        case _: StorageEvent.DoNothingEvent => UIO.unit
        case copyEvent: StorageEvent.CopyEvent => {
          val sourceKey = copyEvent.sourceKey
          val targetKey = copyEvent.targetKey
          Console.putMessageLnB(CopyComplete(sourceKey, targetKey), batchMode)
        }
        case uploadEvent: StorageEvent.UploadEvent => {
          val remoteKey = uploadEvent.remoteKey
          ProgressUI.finishedUploading(remoteKey) *>
            Console.putMessageLnB(UploadComplete(remoteKey), batchMode)
        }
        case deleteEvent: StorageEvent.DeleteEvent => {
          val remoteKey = deleteEvent.remoteKey
          Console.putMessageLnB(DeleteComplete(remoteKey), batchMode)
        }
        case errorEvent: StorageEvent.ErrorEvent => {
          val remoteKey = errorEvent.remoteKey
          val action    = errorEvent.action
          val e         = errorEvent.e
          ProgressUI.finishedUploading(remoteKey) *>
            Console.putMessageLnB(ErrorQueueEventOccurred(action, e), batchMode)
        }
        case _: StorageEvent.ShutdownEvent => UIO.unit
      }
    } yield ()
  }

  private def uploadWaitComplete(action: Action): ZIO[Console, Nothing, Unit] =
    Console.putStrLn(s"Finished waiting to other upload - now $action")

  private def awaitingUpload(remoteKey: RemoteKey,
                             hash: MD5Hash): ZIO[Console, Nothing, Unit] =
    Console.putStrLn(
      s"Awaiting another upload of $hash before copying it to $remoteKey")

  private def fileFound(configuration: Configuration,
                        localFile: LocalFile): ZIO[Console, Nothing, Unit] =
    ZIO.when(configuration.batchMode)(
      Console.putStrLn(s"Found: ${localFile.file}"))

  private def showSummary(counters: Counters): ZIO[Console, Nothing, Unit] =
    Console.putStrLn(eraseToEndOfScreen) *>
      Console.putStrLn(s"Uploaded ${counters.uploaded} files") *>
      Console.putStrLn(s"Copied   ${counters.copied} files") *>
      Console.putStrLn(s"Deleted  ${counters.deleted} files") *>
      Console.putStrLn(s"Errors   ${counters.errors}")

  private def remoteDataFetched(size: Int): ZIO[Console, Nothing, Unit] =
    Console.putStrLn(s"Found $size remote objects")

  private def showValidConfig(
      configuration: Configuration): ZIO[Console, Nothing, Unit] =
    Console.putMessageLn(
      ConsoleOut.ValidConfig(configuration.bucket,
                             configuration.prefix,
                             configuration.sources))

  def trimHead(str: String): String = {
    val width = Terminal.width
    str.length match {
      case l if l > width => str.substring(l - width)
      case _              => str
    }
  }

  def actionChosen(configuration: Configuration,
                   action: Action): ZIO[Console, Nothing, Unit] = {
    val message = trimHead(action.asString()) + eraseLineForward
    val batch   = configuration.batchMode
    for {
      _ <- ZIO.when(!batch) { Console.putStr(message + "\r") }
      _ <- ZIO.when(batch) { Console.putStrLn(message) }
    } yield ()
  }

}
