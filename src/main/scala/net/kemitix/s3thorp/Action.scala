package net.kemitix.s3thorp

sealed trait Action
case class DoNothing(remoteKey: RemoteKey) extends Action
case class ToUpload(localFile: LocalFile) extends Action
case class ToCopy(sourceKey: RemoteKey,
                  hash: MD5Hash,
                  targetKey: RemoteKey) extends Action
case class ToDelete(remoteKey: RemoteKey) extends Action
