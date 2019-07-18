package net.kemitix.thorp.core

import monocle.macros.Lenses
import net.kemitix.thorp.domain.{Bucket, LocalFile, MD5Hash, RemoteKey}

sealed trait Action {
  def bucket: Bucket
  def size: Long
}
object Action {

  @Lenses
  final case class DoNothing(
      bucket: Bucket,
      remoteKey: RemoteKey,
      size: Long
  ) extends Action

  @Lenses
  final case class ToUpload(
      bucket: Bucket,
      localFile: LocalFile,
      size: Long
  ) extends Action

  @Lenses
  final case class ToCopy(
      bucket: Bucket,
      sourceKey: RemoteKey,
      hash: MD5Hash,
      targetKey: RemoteKey,
      size: Long
  ) extends Action

  @Lenses
  final case class ToDelete(
      bucket: Bucket,
      remoteKey: RemoteKey,
      size: Long
  ) extends Action

}
