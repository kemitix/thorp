package net.kemitix.thorp.core

import net.kemitix.thorp.domain.{Bucket, LocalFile, MD5Hash, RemoteKey}

sealed trait Action {
  def bucket: Bucket
}
object Action {

  final case class DoNothing(bucket: Bucket,
                             remoteKey: RemoteKey) extends Action

  final case class ToUpload(bucket: Bucket,
                            localFile: LocalFile) extends Action

  final case class ToCopy(bucket: Bucket,
                          sourceKey: RemoteKey,
                          hash: MD5Hash,
                          targetKey: RemoteKey) extends Action

  final case class ToDelete(bucket: Bucket,
                            remoteKey: RemoteKey) extends Action

}
