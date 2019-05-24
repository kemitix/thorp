package net.kemitix.s3thorp

sealed trait S3Action {

  // the remote key that was uploaded, deleted or otherwise updated by the action
  def remoteKey: RemoteKey

  val order: Int

}

case class DoNothingS3Action(remoteKey: RemoteKey) extends S3Action {
  override val order: Int = 0
}

case class CopyS3Action(remoteKey: RemoteKey) extends S3Action {
  override val order: Int = 1
}
case class UploadS3Action(remoteKey: RemoteKey,
                          md5Hash: MD5Hash) extends S3Action {
  override val order: Int = 2
}
case class DeleteS3Action(remoteKey: RemoteKey) extends S3Action {
  override val order: Int = 3
}

object S3Action {
  implicit def ord[A <: S3Action]: Ordering[A] = Ordering.by(_.order)
}
