package net.kemitix.s3thorp

import java.io.File
import java.nio.file.Paths

final case class RemoteKey(key: String) {
  def asFile(implicit c: Config): File =
    c.source.toPath.resolve(Paths.get(c.prefix.key).relativize(Paths.get(key))).toFile
  def isMissingLocally(implicit c: Config): Boolean =
    ! asFile.exists
}
