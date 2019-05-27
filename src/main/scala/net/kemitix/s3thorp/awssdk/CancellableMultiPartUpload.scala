package net.kemitix.s3thorp.awssdk

case class CancellableMultiPartUpload(e: Throwable,
                                      uploadId: String) extends Exception(e)
