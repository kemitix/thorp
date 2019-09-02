package net.kemitix.thorp.domain

import java.io.File
import java.nio.file.{Path, Paths}
import Implicits._

final case class RemoteKey(key: String)

object RemoteKey {
  val key: SimpleLens[RemoteKey, String] =
    SimpleLens[RemoteKey, String](_.key, b => a => b.copy(key = a))
  def asFile(source: Path, prefix: RemoteKey)(
      remoteKey: RemoteKey): Option[File] =
    if (remoteKey.key.length === 0) None
    else Some(source.resolve(RemoteKey.relativeTo(prefix)(remoteKey)).toFile)
  def relativeTo(prefix: RemoteKey)(remoteKey: RemoteKey): Path = {
    prefix match {
      case RemoteKey("") => Paths.get(remoteKey.key)
      case _             => Paths.get(prefix.key).relativize(Paths.get(remoteKey.key))
    }
  }
  def resolve(path: String)(remoteKey: RemoteKey): RemoteKey =
    RemoteKey(List(remoteKey.key, path).filterNot(_.isEmpty).mkString("/"))
  def fromSourcePath(source: Path, path: Path): RemoteKey = {
    RemoteKey(source.relativize(path).toString)
  }
}
