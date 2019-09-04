package net.kemitix.throp.uishell

import net.kemitix.thorp.domain.{
  Action,
  Counters,
  LocalFile,
  MD5Hash,
  RemoteKey,
  StorageQueueEvent
}

sealed trait UIEvent
object UIEvent {
  case object ShowValidConfig extends UIEvent

  case class RemoteDataFetched(size: Int) extends UIEvent

  case class ShowSummary(counters: Counters) extends UIEvent

  case class FileFound(localFile: LocalFile) extends UIEvent

  case class ActionChosen(action: Action) extends UIEvent

  /**
    * The content of the file ({{hash}}) that will be placed
    * at {{remoteKey}} is already being uploaded to another
    * location. Once that upload has completed, its RemoteKey
    * will become available and a Copy action can be made.
    * @param remoteKey where this upload will copy the other to
    * @param hash the hash of the file being uploaded
    */
  case class AwaitingAnotherUpload(remoteKey: RemoteKey, hash: MD5Hash)
      extends UIEvent

  case class AnotherUploadWaitComplete(action: Action) extends UIEvent

  case class ActionFinished(event: StorageQueueEvent,
                            actionCounter: Int,
                            bytesCounter: Long)
      extends UIEvent

}
