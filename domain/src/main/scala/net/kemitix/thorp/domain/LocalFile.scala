package net.kemitix.thorp.domain

import java.io.File
import java.nio.file.Path

final case class LocalFile(file: File, source: File, hashes: Map[String, MD5Hash], remoteKey: RemoteKey) {

  require(!file.isDirectory, s"LocalFile must not be a directory: $file")

  def isDirectory: Boolean = file.isDirectory

  // the path of the file within the source
  def relative: Path = source.toPath.relativize(file.toPath)

  def matches(other: MD5Hash): Boolean = hashes.values.exists(other equals _)

  def md5base64: Option[String] = hashes.get("md5").map(_.hash64)

}

object LocalFile {
  def resolve(path: String,
              md5Hashes: Map[String, MD5Hash],
              source: File,
              fileToKey: File => RemoteKey): LocalFile = {
    val file = source.toPath.resolve(path).toFile
    LocalFile(file, source, md5Hashes, fileToKey(file))
  }
}
