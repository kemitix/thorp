package net.kemitix.thorp.domain

import java.io.File
import java.nio.file.Path

import net.kemitix.thorp.domain.HashType.MD5

final case class LocalFile private (
    file: File,
    source: File,
    hashes: Map[HashType, MD5Hash],
    remoteKey: RemoteKey
)

object LocalFile {
  val remoteKey: SimpleLens[LocalFile, RemoteKey] =
    SimpleLens[LocalFile, RemoteKey](_.remoteKey,
                                     b => a => b.copy(remoteKey = a))
  // the path of the file within the source
  def relativeToSource(localFile: LocalFile): Path =
    localFile.source.toPath.relativize(localFile.file.toPath)
  def matchesHash(localFile: LocalFile)(other: MD5Hash): Boolean =
    localFile.hashes.values.exists(other equals _)
  def md5base64(localFile: LocalFile): Option[String] =
    localFile.hashes.get(MD5).map(_.hash64)
}
