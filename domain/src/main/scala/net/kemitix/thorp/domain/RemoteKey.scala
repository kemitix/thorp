package net.kemitix.thorp.domain

import java.io.File
import java.nio.file.Paths

final case class RemoteKey(key: String) {

  def asFile(source: File, prefix: RemoteKey): File =
    source.toPath.resolve(relativeTo(prefix)).toFile

  private def relativeTo(prefix: RemoteKey) = {
    prefix match {
      case RemoteKey("") => Paths.get(prefix.key)
      case _ => Paths.get(prefix.key).relativize(Paths.get(key))
    }
  }

  def isMissingLocally(source: File, prefix: RemoteKey): Boolean =
    ! asFile(source, prefix).exists

  def resolve(path: String): RemoteKey =
    RemoteKey(key + "/" + path)

}
