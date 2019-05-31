package net.kemitix.s3thorp.awssdk

final case class CancellableMultiPartUpload(
  e: Throwable,
  uploadId: String) extends Exception(e)
