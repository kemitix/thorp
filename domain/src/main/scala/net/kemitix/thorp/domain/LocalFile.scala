package net.kemitix.thorp.domain

import java.io.File
import java.nio.file.Path

import net.kemitix.thorp.domain.HashType.MD5

final case class LocalFile private (
    file: File,
    source: File,
    hashes: Map[HashType, MD5Hash],
    remoteKey: RemoteKey
) {

  def md5base64: Option[String] = hashes.get(MD5).map(_.hash64)

}

object LocalFile {
  val remoteKey: SimpleLens[LocalFile, RemoteKey] =
    SimpleLens[LocalFile, RemoteKey](_.remoteKey,
                                     b => a => b.copy(remoteKey = a))
  // the path of the file within the source
  def relativeToSource(localFile: LocalFile): Path =
    localFile.source.toPath.relativize(localFile.file.toPath)
  def matchesHash(localFile: LocalFile)(other: MD5Hash): Boolean =
    localFile.hashes.values.exists(other equals _)
}
