package net.kemitix.thorp.domain

sealed trait StorageQueueEvent

object StorageQueueEvent {

  final case class DoNothingQueueEvent(
      remoteKey: RemoteKey
  ) extends StorageQueueEvent

  final case class CopyQueueEvent(
      sourceKey: RemoteKey,
      targetKey: RemoteKey
  ) extends StorageQueueEvent

  final case class UploadQueueEvent(
      remoteKey: RemoteKey,
      md5Hash: MD5Hash
  ) extends StorageQueueEvent

  final case class DeleteQueueEvent(
      remoteKey: RemoteKey
  ) extends StorageQueueEvent

  final case class ErrorQueueEvent(
      action: Action,
      remoteKey: RemoteKey,
      e: Throwable
  ) extends StorageQueueEvent

  final case class ShutdownQueueEvent() extends StorageQueueEvent

  sealed trait Action {
    val name: String
    val keys: String
  }
  object Action {
    final case class Copy(keys: String) extends Action {
      override val name: String = "Copy"
    }
    final case class Upload(keys: String) extends Action {
      override val name: String = "Upload"
    }
    final case class Delete(keys: String) extends Action {
      override val name: String = "Delete"
    }
  }

}
