package net.kemitix.s3thorp

sealed trait Action
final case class DoNothing(remoteKey: RemoteKey) extends Action
final case class ToUpload(localFile: LocalFile) extends Action
final case class ToCopy(
  sourceKey: RemoteKey,
  hash: MD5Hash,
  targetKey: RemoteKey) extends Action
final case class ToDelete(remoteKey: RemoteKey) extends Action
