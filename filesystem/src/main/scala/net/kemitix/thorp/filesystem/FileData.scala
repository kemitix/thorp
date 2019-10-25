package net.kemitix.thorp.filesystem

import net.kemitix.thorp.domain.{HashType, LastModified, MD5Hash}

case class FileData(
    hashes: Hasher
) {}

object FileData {
  def create(hashType: HashType,
             hash: MD5Hash,
             lastModified: LastModified): FileData = ???
}
