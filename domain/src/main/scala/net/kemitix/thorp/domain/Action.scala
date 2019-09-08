package net.kemitix.thorp.domain

sealed trait Action {
  def bucket: Bucket
  def size: Long
  def remoteKey: RemoteKey
}
object Action {

  final case class DoNothing(
      bucket: Bucket,
      remoteKey: RemoteKey,
      size: Long
  ) extends Action

  final case class ToUpload(
      bucket: Bucket,
      localFile: LocalFile,
      size: Long
  ) extends Action {
    override def remoteKey: RemoteKey = localFile.remoteKey
  }

  final case class ToCopy(
      bucket: Bucket,
      sourceKey: RemoteKey,
      hash: MD5Hash,
      targetKey: RemoteKey,
      size: Long
  ) extends Action {
    override def remoteKey: RemoteKey = targetKey
  }

  final case class ToDelete(
      bucket: Bucket,
      remoteKey: RemoteKey,
      size: Long
  ) extends Action

}
