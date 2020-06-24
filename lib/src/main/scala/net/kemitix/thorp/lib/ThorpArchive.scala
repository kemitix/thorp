package net.kemitix.thorp.lib

import net.kemitix.thorp.config.Configuration
import net.kemitix.thorp.console._
import net.kemitix.thorp.domain.StorageEvent._
import net.kemitix.thorp.domain.{Channel, StorageEvent}
import net.kemitix.thorp.uishell.UIEvent

trait ThorpArchive {

  def update(configuration: Configuration,
             uiSink: Channel.Sink[UIEvent],
             sequencedAction: SequencedAction,
             totalBytesSoFar: Long): StorageEvent

  def logEvent(configuration: Configuration,
               event: StorageEvent): StorageEvent = {
    val batchMode = configuration.batchMode
    event match {
      case uploadEvent: UploadEvent =>
        val remoteKey = uploadEvent.remoteKey
        Console.putMessageLnB(ConsoleOut.uploadComplete(remoteKey), batchMode)
      case copyEvent: CopyEvent =>
        val sourceKey = copyEvent.sourceKey
        val targetKey = copyEvent.targetKey
        Console.putMessageLnB(
          ConsoleOut.copyComplete(sourceKey, targetKey),
          batchMode
        )
      case deleteEvent: DeleteEvent =>
        val remoteKey = deleteEvent.remoteKey
        Console.putMessageLnB(ConsoleOut.deleteComplete(remoteKey), batchMode)
      case errorEvent: ErrorEvent =>
        val action = errorEvent.action
        val e = errorEvent.e
        Console.putMessageLnB(
          ConsoleOut.errorQueueEventOccurred(action, e),
          batchMode
        )
    }
    event
  }

}
