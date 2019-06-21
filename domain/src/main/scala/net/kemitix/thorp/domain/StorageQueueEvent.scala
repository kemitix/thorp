package net.kemitix.thorp.domain

sealed trait StorageQueueEvent {

  // the remote key that was uploaded, deleted or otherwise updated by the action
  def remoteKey: RemoteKey

  val order: Int

}

object StorageQueueEvent {

  final case class DoNothingQueueEvent(remoteKey: RemoteKey) extends StorageQueueEvent {
    override val order: Int = 0
  }

  final case class CopyQueueEvent(remoteKey: RemoteKey) extends StorageQueueEvent {
    override val order: Int = 1
  }

  final case class UploadQueueEvent(remoteKey: RemoteKey,
                                    md5Hash: MD5Hash) extends StorageQueueEvent {
    override val order: Int = 2
  }

  final case class DeleteQueueEvent(remoteKey: RemoteKey) extends StorageQueueEvent {
    override val order: Int = 3
  }

  final case class ErrorQueueEvent(remoteKey: RemoteKey, e: Throwable) extends StorageQueueEvent {
    override val order: Int = 10
  }

  implicit def ord[A <: StorageQueueEvent]: Ordering[A] = Ordering.by(_.order)

}
