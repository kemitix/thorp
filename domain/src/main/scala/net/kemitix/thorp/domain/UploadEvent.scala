package net.kemitix.thorp.domain

import monocle.macros.Lenses

sealed trait UploadEvent {
  def name: String
}

object UploadEvent {

  @Lenses
  final case class TransferEvent(
      name: String
  ) extends UploadEvent

  @Lenses
  final case class RequestEvent(
      name: String,
      bytes: Long,
      transferred: Long
  ) extends UploadEvent

  @Lenses
  final case class ByteTransferEvent(
      name: String
  ) extends UploadEvent

}
