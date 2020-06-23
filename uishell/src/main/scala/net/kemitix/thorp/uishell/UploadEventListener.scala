package net.kemitix.thorp.uishell

import java.util.concurrent.atomic.AtomicLong

import net.kemitix.thorp.domain.LocalFile
import net.kemitix.thorp.domain.MessageChannel.MessageConsumer
import net.kemitix.thorp.uishell.UploadProgressEvent.RequestEvent

object UploadEventListener {

  final case class Settings(uiChannel: MessageConsumer[UIEvent],
                            localFile: LocalFile,
                            index: Int,
                            totalBytesSoFar: Long,
                            batchMode: Boolean)

  def listener(settings: Settings): UploadProgressEvent => Unit = {
    val bytesTransferred = new AtomicLong(0L)
    event =>
      {
        event match {
          case e: RequestEvent =>
            settings.uiChannel.accept(
              UIEvent.requestCycle(
                settings.localFile,
                bytesTransferred.addAndGet(e.transferred),
                settings.index,
                settings.totalBytesSoFar
              )
            )
          case _ => ()
        }
      }
  }

}
