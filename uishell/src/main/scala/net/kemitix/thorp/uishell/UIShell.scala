package net.kemitix.thorp.uishell

import net.kemitix.thorp.config.Configuration
import net.kemitix.thorp.console.{Console, ConsoleOut}
import net.kemitix.thorp.domain.Channel.Listener
import net.kemitix.thorp.domain.Terminal.{eraseLineForward, eraseToEndOfScreen}
import net.kemitix.thorp.domain._

object UIShell {

  def receiver(configuration: Configuration): Listener[UIEvent] = {
    case _: UIEvent.ShowValidConfig     => showValidConfig(configuration)
    case uie: UIEvent.RemoteDataFetched => remoteDataFetched(uie.size)
    case uie: UIEvent.ShowSummary       => showSummary(uie.counters)
    case uie: UIEvent.FileFound =>
      fileFound(configuration, uie.localFile)
    case uie: UIEvent.ActionChosen =>
      actionChosen(configuration, uie.action)
    case uie: UIEvent.AwaitingAnotherUpload =>
      awaitingUpload(uie.remoteKey, uie.hash)
    case uie: UIEvent.AnotherUploadWaitComplete =>
      uploadWaitComplete(uie.action)
    case uie: UIEvent.ActionFinished =>
      actionFinished(configuration, uie.event)
    case _: UIEvent.KeyFound => ()
    case uie: UIEvent.RequestCycle =>
      ProgressUI.requestCycle(
        configuration,
        uie.localFile,
        uie.bytesTransferred,
        uie.index,
        uie.totalBytesSoFar
      )
  }

  private def actionFinished(configuration: Configuration,
                             event: StorageEvent): Unit =
    event match {
      case _: StorageEvent.DoNothingEvent => ()
      case copyEvent: StorageEvent.CopyEvent =>
        val sourceKey = copyEvent.sourceKey
        val targetKey = copyEvent.targetKey
        Console.putMessageLnB(
          ConsoleOut.copyComplete(sourceKey, targetKey),
          configuration.batchMode
        )
      case uploadEvent: StorageEvent.UploadEvent =>
        val remoteKey = uploadEvent.remoteKey
        Console
          .putMessageLnB(
            ConsoleOut.uploadComplete(remoteKey),
            configuration.batchMode
          )
        ProgressUI.finishedUploading(remoteKey)
      case deleteEvent: StorageEvent.DeleteEvent =>
        val remoteKey = deleteEvent.remoteKey
        Console.putMessageLnB(
          ConsoleOut.deleteComplete(remoteKey),
          configuration.batchMode
        )
      case errorEvent: StorageEvent.ErrorEvent =>
        val remoteKey = errorEvent.remoteKey
        val action = errorEvent.action
        val e = errorEvent.e
        ProgressUI.finishedUploading(remoteKey)
        Console.putMessageLnB(
          ConsoleOut.errorQueueEventOccurred(action, e),
          configuration.batchMode
        )
      case _: StorageEvent.ShutdownEvent => ()
    }

  private def uploadWaitComplete(action: Action): Unit =
    Console.putStrLn(s"Finished waiting to other upload - now $action")

  private def awaitingUpload(remoteKey: RemoteKey, hash: MD5Hash): Unit =
    Console.putStrLn(
      s"Awaiting another upload of $hash before copying it to $remoteKey"
    )

  private def fileFound(configuration: Configuration,
                        localFile: LocalFile): Unit =
    if (configuration.batchMode) {
      Console.putStrLn(s"Found: ${localFile.file}")
    }

  private def showSummary(counters: Counters): Unit = {
    Console.putStrLn(eraseToEndOfScreen)
    Console.putStrLn(s"Uploaded ${counters.uploaded} files")
    Console.putStrLn(s"Copied   ${counters.copied} files")
    Console.putStrLn(s"Deleted  ${counters.deleted} files")
    Console.putStrLn(s"Errors   ${counters.errors}")
  }

  private def remoteDataFetched(size: Int): Unit =
    Console.putStrLn(s"Found $size remote objects")

  private def showValidConfig(configuration: Configuration): Unit =
    Console.putMessageLn(
      ConsoleOut.validConfig(
        configuration.bucket,
        configuration.prefix,
        configuration.sources
      )
    )

  def trimHead(str: String): String = {
    val width = Terminal.width
    str.length match {
      case l if l > width => str.substring(l - width)
      case _              => str
    }
  }

  def actionChosen(configuration: Configuration, action: Action): Unit = {
    val message = trimHead(action.asString()) + eraseLineForward
    if (configuration.batchMode) Console.putStr(message + "\r")
    else Console.putStrLn(message)
  }

}
