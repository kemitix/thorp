package net.kemitix.thorp.uishell

import java.util.concurrent.atomic.AtomicLong

import net.kemitix.eip.zio.Message
import net.kemitix.eip.zio.MessageChannel.UChannel
import net.kemitix.thorp.domain.LocalFile
import net.kemitix.thorp.uishell.UploadProgressEvent.RequestEvent

object UploadEventListener {

  final case class Settings(uiChannel: UChannel[Any, UIEvent],
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
            settings.uiChannel(
              Message.withBody(
                UIEvent.requestCycle(
                  settings.localFile,
                  bytesTransferred.addAndGet(e.transferred),
                  settings.index,
                  settings.totalBytesSoFar
                )
              )
            )
          case _ => ()
        }
      }
  }

}
