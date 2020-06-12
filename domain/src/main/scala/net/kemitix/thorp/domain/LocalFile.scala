package net.kemitix.thorp.domain

import java.io.File

import scala.jdk.OptionConverters._

import net.kemitix.thorp.domain.HashType.MD5

final case class LocalFile private (
    file: File,
    source: File,
    hashes: Hashes,
    remoteKey: RemoteKey,
    length: Long
)

object LocalFile {
  val remoteKey: SimpleLens[LocalFile, RemoteKey] =
    SimpleLens[LocalFile, RemoteKey](_.remoteKey,
                                     b => a => b.copy(remoteKey = a))
  def matchesHash(localFile: LocalFile)(other: MD5Hash): Boolean =
    localFile.hashes.values.contains(other)
  def md5base64(localFile: LocalFile): Option[String] =
    localFile.hashes.get(MD5).toScala.map(h => h.hash64())
}
