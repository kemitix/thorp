package net.kemitix.thorp.core

import net.kemitix.thorp.domain.{Bucket, LocalFile, MD5Hash, RemoteKey}

sealed trait Action {
  def bucket: Bucket
  def size: Long
}
object Action {

  final case class DoNothing(bucket: Bucket,
                             remoteKey: RemoteKey,
                             size: Long) extends Action

  final case class ToUpload(bucket: Bucket,
                            localFile: LocalFile,
                            size: Long) extends Action

  final case class ToCopy(bucket: Bucket,
                          sourceKey: RemoteKey,
                          hash: MD5Hash,
                          targetKey: RemoteKey,
                          size: Long) extends Action

  final case class ToDelete(bucket: Bucket,
                            remoteKey: RemoteKey,
                            size: Long) extends Action

}
