package net.kemitix.s3thorp

import java.io.File
import java.nio.file.Path

final case class LocalFile(
  file: File,
  source: File,
  keyGenerator: File => RemoteKey,
  suppliedHash: Option[MD5Hash] = None)
  (implicit c: Config)
  extends MD5HashGenerator {

  require(!file.isDirectory, s"LocalFile must not be a directory: $file")

  private lazy val myhash = suppliedHash.getOrElse(md5File(file))

  def hash: MD5Hash = myhash

  // the equivalent location of the file on S3
  def remoteKey: RemoteKey = keyGenerator(file)

  def isDirectory: Boolean = file.isDirectory

  // the path of the file within the source
  def relative: Path = source.toPath.relativize(file.toPath)

}
