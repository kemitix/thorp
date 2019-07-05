package net.kemitix.thorp.domain

import java.io.File
import java.nio.file.{Path, Paths}

final case class RemoteKey(key: String) {

  def asFile(source: Path, prefix: RemoteKey): File =
    source.resolve(relativeTo(prefix)).toFile

  private def relativeTo(prefix: RemoteKey) = {
    prefix match {
      case RemoteKey("") => Paths.get(prefix.key)
      case _ => Paths.get(prefix.key).relativize(Paths.get(key))
    }
  }

  def isMissingLocally(sources: Sources, prefix: RemoteKey): Boolean =
    !sources.paths.exists(source => asFile(source, prefix).exists)

  def resolve(path: String): RemoteKey =
    RemoteKey(key + "/" + path)

}
