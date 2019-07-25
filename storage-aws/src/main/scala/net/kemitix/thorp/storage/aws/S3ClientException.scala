package net.kemitix.thorp.storage.aws

sealed trait S3ClientException extends Throwable

object S3ClientException {
  case object HashError extends S3ClientException {
    override def getMessage: String =
      "The hash of the object to be overwritten did not match the the expected value"
  }
  case class CopyError(error: Throwable) extends S3ClientException {
    override def getMessage: String =
      "The hash of the object to be overwritten did not match the the expected value"
  }
  case class S3Exception(message: String) extends S3ClientException {
    override def getMessage: String = message
  }
}
