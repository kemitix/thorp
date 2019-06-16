package net.kemitix.thorp.domain

import java.io.File
import java.nio.file.Path

final case class LocalFile(file: File, source: File, hash: MD5Hash, keyGenerator: File => RemoteKey) {

  require(!file.isDirectory, s"LocalFile must not be a directory: $file")

  // the equivalent location of the file on S3
  def remoteKey: RemoteKey = keyGenerator(file)

  def isDirectory: Boolean = file.isDirectory

  // the path of the file within the source
  def relative: Path = source.toPath.relativize(file.toPath)

}

object LocalFile {
  def resolve(path: String,
              md5Hash: MD5Hash,
              source: File,
              fileToKey: File => RemoteKey): LocalFile = {
    val file = source.toPath.resolve(path).toFile
    LocalFile(file, source, md5Hash, fileToKey)
  }
}
