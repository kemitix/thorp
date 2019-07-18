package net.kemitix.thorp.domain

import monocle.macros.Lenses

sealed trait StorageQueueEvent {

  val order: Int

}

object StorageQueueEvent {

  @Lenses
  final case class DoNothingQueueEvent(
      remoteKey: RemoteKey
  ) extends StorageQueueEvent {
    override val order: Int = 0
  }

  @Lenses
  final case class CopyQueueEvent(
      remoteKey: RemoteKey
  ) extends StorageQueueEvent {
    override val order: Int = 1
  }

  @Lenses
  final case class UploadQueueEvent(
      remoteKey: RemoteKey,
      md5Hash: MD5Hash
  ) extends StorageQueueEvent {
    override val order: Int = 2
  }

  @Lenses
  final case class DeleteQueueEvent(
      remoteKey: RemoteKey
  ) extends StorageQueueEvent {
    override val order: Int = 3
  }

  @Lenses
  final case class ErrorQueueEvent(
      remoteKey: RemoteKey,
      e: Throwable
  ) extends StorageQueueEvent {
    override val order: Int = 10
  }

  @Lenses
  final case class ShutdownQueueEvent() extends StorageQueueEvent {
    override val order: Int = 99
  }

  implicit def ord[A <: StorageQueueEvent]: Ordering[A] = Ordering.by(_.order)

}
