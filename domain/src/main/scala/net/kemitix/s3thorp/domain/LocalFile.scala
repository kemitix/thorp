package net.kemitix.s3thorp.domain

import java.io.File
import java.nio.file.Path

final case class LocalFile(
  file: File,
  source: File,
  keyGenerator: File => RemoteKey,
  md5HashGenerator: File => MD5Hash,
  suppliedHash: Option[MD5Hash] = None) {

  require(!file.isDirectory, s"LocalFile must not be a directory: $file")

  private lazy val myhash = suppliedHash.getOrElse(md5HashGenerator(file))

  def hash: MD5Hash = myhash

  // the equivalent location of the file on S3
  def remoteKey: RemoteKey = keyGenerator(file)

  def isDirectory: Boolean = file.isDirectory

  // the path of the file within the source
  def relative: Path = source.toPath.relativize(file.toPath)

}

object LocalFile {
  def resolve(path: String,
              myHash: MD5Hash,
              source: File,
              fileToKey: File => RemoteKey,
              fileToHash: File => MD5Hash): LocalFile =
    LocalFile(
      file = source.toPath.resolve(path).toFile,
      source = source,
      keyGenerator = fileToKey,
      md5HashGenerator = fileToHash,
      suppliedHash = Some(myHash))
}
