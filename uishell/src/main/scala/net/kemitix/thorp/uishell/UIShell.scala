package net.kemitix.thorp.uishell

import net.kemitix.eip.zio.MessageChannel
import net.kemitix.thorp.config.Configuration
import net.kemitix.thorp.console.{Console, ConsoleOut}
import net.kemitix.thorp.domain.Terminal.{eraseLineForward, eraseToEndOfScreen}
import net.kemitix.thorp.domain._
import zio.{UIO, ZIO}

object UIShell {

  def receiver(
    configuration: Configuration
  ): UIO[MessageChannel.UReceiver[Any, UIEvent]] =
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
        case UIEvent.RequestCycle(
            localFile,
            bytesTransferred,
            index,
            totalBytesSoFar
            ) =>
          ProgressUI.requestCycle(
            configuration,
            localFile,
            bytesTransferred,
            index,
            totalBytesSoFar
          )
      }
    }

  private def actionFinished(configuration: Configuration,
                             event: StorageEvent): UIO[Unit] = {
    val batchMode = configuration.batchMode
    for {
      _ <- event match {
        case _: StorageEvent.DoNothingEvent => UIO.unit
        case copyEvent: StorageEvent.CopyEvent =>
          val sourceKey = copyEvent.sourceKey
          val targetKey = copyEvent.targetKey
          Console.putMessageLnB(
            ConsoleOut.copyComplete(sourceKey, targetKey),
            batchMode
          )
          UIO.unit
        case uploadEvent: StorageEvent.UploadEvent =>
          val remoteKey = uploadEvent.remoteKey
          Console
            .putMessageLnB(ConsoleOut.uploadComplete(remoteKey), batchMode)
          ProgressUI.finishedUploading(remoteKey)
        case deleteEvent: StorageEvent.DeleteEvent =>
          val remoteKey = deleteEvent.remoteKey
          Console.putMessageLnB(ConsoleOut.deleteComplete(remoteKey), batchMode)
          UIO.unit
        case errorEvent: StorageEvent.ErrorEvent =>
          val remoteKey = errorEvent.remoteKey
          val action = errorEvent.action
          val e = errorEvent.e
          ProgressUI.finishedUploading(remoteKey) *>
            UIO(
              Console.putMessageLnB(
                ConsoleOut.errorQueueEventOccurred(action, e),
                batchMode
              )
            )
        case _: StorageEvent.ShutdownEvent => UIO.unit
      }
    } yield ()
  }

  private def uploadWaitComplete(action: Action): UIO[Unit] = {
    Console.putStrLn(s"Finished waiting to other upload - now $action")
    UIO.unit
  }

  private def awaitingUpload(remoteKey: RemoteKey, hash: MD5Hash): UIO[Unit] = {
    Console.putStrLn(
      s"Awaiting another upload of $hash before copying it to $remoteKey"
    )
    UIO.unit
  }
  private def fileFound(configuration: Configuration,
                        localFile: LocalFile): UIO[Unit] =
    ZIO.when(configuration.batchMode) {
      Console.putStrLn(s"Found: ${localFile.file}")
      UIO.unit
    }

  private def showSummary(counters: Counters): UIO[Unit] = {
    Console.putStrLn(eraseToEndOfScreen)
    Console.putStrLn(s"Uploaded ${counters.uploaded} files")
    Console.putStrLn(s"Copied   ${counters.copied} files")
    Console.putStrLn(s"Deleted  ${counters.deleted} files")
    Console.putStrLn(s"Errors   ${counters.errors}")
    UIO.unit
  }

  private def remoteDataFetched(size: Int): UIO[Unit] = {
    Console.putStrLn(s"Found $size remote objects")
    UIO.unit
  }

  private def showValidConfig(configuration: Configuration): UIO[Unit] = {
    Console.putMessageLn(
      ConsoleOut.validConfig(
        configuration.bucket,
        configuration.prefix,
        configuration.sources
      )
    )
    UIO.unit
  }

  def trimHead(str: String): String = {
    val width = Terminal.width
    str.length match {
      case l if l > width => str.substring(l - width)
      case _              => str
    }
  }

  def actionChosen(configuration: Configuration, action: Action): UIO[Unit] = {
    val message = trimHead(action.asString()) + eraseLineForward
    if (configuration.batchMode) Console.putStr(message + "\r")
    else Console.putStrLn(message)
    UIO.unit
  }

}
