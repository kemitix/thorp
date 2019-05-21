package net.kemitix.s3thorp

sealed trait S3Action {

  // the remote key that was uploaded, deleted or otherwise updated by the action
  def remoteKey: RemoteKey

}

case class UploadS3Action(remoteKey: RemoteKey,
                          md5Hash: MD5Hash) extends S3Action
case class CopyS3Action(remoteKey: RemoteKey) extends S3Action
case class DeleteS3Action(remoteKey: RemoteKey) extends S3Action