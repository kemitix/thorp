package net.kemitix.thorp.uishell

sealed trait UploadProgressEvent {
  def name: String
}

object UploadProgressEvent {

  final case class TransferEvent(
      name: String
  ) extends UploadProgressEvent

  final case class RequestEvent(
      name: String,
      bytes: Long,
      transferred: Long
  ) extends UploadProgressEvent

  final case class ByteTransferEvent(
      name: String
  ) extends UploadProgressEvent

}
