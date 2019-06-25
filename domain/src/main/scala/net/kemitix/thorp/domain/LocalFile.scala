package net.kemitix.thorp.domain

import java.io.File
import java.nio.file.Path

final case class LocalFile(file: File, source: File, hash: MD5Hash, remoteKey: RemoteKey) {

  require(!file.isDirectory, s"LocalFile must not be a directory: $file")

  def isDirectory: Boolean = file.isDirectory

  // the path of the file within the source
  def relative: Path = source.toPath.relativize(file.toPath)

  def matches(other: MD5Hash): Boolean = hash.hash == other.hash

}

object LocalFile {
  def resolve(path: String,
              md5Hash: MD5Hash,
              source: File,
              fileToKey: File => RemoteKey): LocalFile = {
    val file = source.toPath.resolve(path).toFile
    LocalFile(file, source, md5Hash, fileToKey(file))
  }
}
