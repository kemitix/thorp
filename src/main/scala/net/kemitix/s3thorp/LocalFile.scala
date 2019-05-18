package net.kemitix.s3thorp

import java.io.File
import java.nio.file.Path

final case class LocalFile(file: File,
                           source: File,
                           keyGenerator: File => RemoteKey)
  extends MD5HashGenerator {

  require(!file.isDirectory, s"LocalFile must not be a directory: $file")

  lazy val hash = md5File(file)

  // the equivalent location of the file on S3
  def remoteKey: RemoteKey = keyGenerator(file)

  def isDirectory: Boolean = file.isDirectory

  // the path of the file within the source
  def relative: Path = source.toPath.relativize(file.toPath)

}
