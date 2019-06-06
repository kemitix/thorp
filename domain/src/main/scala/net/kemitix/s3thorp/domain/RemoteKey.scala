package net.kemitix.s3thorp.domain

import java.io.File
import java.nio.file.Paths

final case class RemoteKey(key: String) {
  def asFile(source: File, prefix: RemoteKey): File =
    source.toPath.resolve(Paths.get(prefix.key).relativize(Paths.get(key))).toFile
  def isMissingLocally(source: File, prefix: RemoteKey): Boolean =
    ! asFile(source, prefix).exists
  def resolve(path: String): RemoteKey =
    RemoteKey(key + "/" + path)
}
