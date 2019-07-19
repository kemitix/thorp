package net.kemitix.thorp.domain

sealed trait UploadEvent {
  def name: String
}

object UploadEvent {

  final case class TransferEvent(
      name: String
  ) extends UploadEvent

  final case class RequestEvent(
      name: String,
      bytes: Long,
      transferred: Long
  ) extends UploadEvent

  final case class ByteTransferEvent(
      name: String
  ) extends UploadEvent

}
