package net.kemitix.s3thorp.awssdk

sealed trait UploadEvent {
  def name: String
}
final case class UploadTransferEvent(name: String) extends UploadEvent
final case class UploadRequestEvent(name: String,
                                    bytes: Long,
                                    transferred: Long) extends UploadEvent
final case class UploadByteTransferEvent(name: String) extends UploadEvent
