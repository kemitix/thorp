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

  // the path of the file within the source
  def relative: Path = source.toPath.relativize(file.toPath)

  def matches(other: MD5Hash): Boolean = hashes.values.exists(other equals _)

  def md5base64: Option[String] = hashes.get(MD5).map(_.hash64)

}

object LocalFile {
  val remoteKey: SimpleLens[LocalFile, RemoteKey] =
    SimpleLens[LocalFile, RemoteKey](_.remoteKey,
                                     b => a => b.copy(remoteKey = a))
}
