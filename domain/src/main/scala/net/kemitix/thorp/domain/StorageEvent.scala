package net.kemitix.thorp.domain

sealed trait StorageEvent

object StorageEvent {

  final case class DoNothingEvent(
      remoteKey: RemoteKey
  ) extends StorageEvent

  final case class CopyEvent(
      sourceKey: RemoteKey,
      targetKey: RemoteKey
  ) extends StorageEvent

  final case class UploadEvent(
      remoteKey: RemoteKey,
      md5Hash: MD5Hash
  ) extends StorageEvent

  final case class DeleteEvent(
      remoteKey: RemoteKey
  ) extends StorageEvent

  final case class ErrorEvent(
      action: Action,
      remoteKey: RemoteKey,
      e: Throwable
  ) extends StorageEvent

  final case class ShutdownEvent() extends StorageEvent

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
