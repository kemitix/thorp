package net.kemitix.s3thorp.aws.lib

final case class CancellableMultiPartUpload(
  e: Throwable,
  uploadId: String) extends Exception(e)
