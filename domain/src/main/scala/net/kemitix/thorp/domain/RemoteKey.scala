package net.kemitix.thorp.domain

import java.io.File
import java.nio.file.{Path, Paths}

import monocle.macros.Lenses

@Lenses
final case class RemoteKey(
    key: String
) {

  def isMissingLocally(
      sources: Sources,
      prefix: RemoteKey
  ): Boolean =
    !sources.paths.exists(source =>
      asFile(source, prefix) match {
        case Some(file) => file.exists
        case None       => false
    })

  def asFile(
      source: Path,
      prefix: RemoteKey
  ): Option[File] =
    if (key.length == 0) None
    else Some(source.resolve(relativeTo(prefix)).toFile)

  private def relativeTo(prefix: RemoteKey) = {
    prefix match {
      case RemoteKey("") => Paths.get(key)
      case _             => Paths.get(prefix.key).relativize(Paths.get(key))
    }
  }

  def resolve(path: String): RemoteKey =
    RemoteKey(List(key, path).filterNot(_.isEmpty).mkString("/"))

}
