package net.kemitix.thorp.domain

sealed trait StorageQueueEvent {

  val order: Int

}

object StorageQueueEvent {

  final case class DoNothingQueueEvent(
      remoteKey: RemoteKey
  ) extends StorageQueueEvent {
    override val order: Int = 0
  }

  final case class CopyQueueEvent(
      sourceKey: RemoteKey,
      targetKey: RemoteKey
  ) extends StorageQueueEvent {
    override val order: Int = 1
  }

  final case class UploadQueueEvent(
      remoteKey: RemoteKey,
      md5Hash: MD5Hash
  ) extends StorageQueueEvent {
    override val order: Int = 2
  }

  final case class DeleteQueueEvent(
      remoteKey: RemoteKey
  ) extends StorageQueueEvent {
    override val order: Int = 3
  }

  final case class ErrorQueueEvent(
      action: Action,
      remoteKey: RemoteKey,
      e: Throwable
  ) extends StorageQueueEvent {
    override val order: Int = 10
  }

  final case class ShutdownQueueEvent() extends StorageQueueEvent {
    override val order: Int = 99
  }

  implicit def ord[A <: StorageQueueEvent]: Ordering[A] = Ordering.by(_.order)

  sealed trait Action {
    val name: String
    val keys: String
  }
  object Action {
    case class Copy(keys: String) extends Action {
      override val name: String = "Copy"
    }
    case class Upload(keys: String) extends Action {
      override val name: String = "Upload"
    }
    case class Delete(keys: String) extends Action {
      override val name: String = "Delete"
    }
  }

}
