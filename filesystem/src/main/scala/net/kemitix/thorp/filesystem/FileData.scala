package net.kemitix.thorp.filesystem

import net.kemitix.thorp.domain.{HashType, Hashes, LastModified, MD5Hash}

case class FileData(
    hashes: Hashes,
    lastModified: LastModified
) {}

object FileData {
  def create(hashType: HashType,
             hash: MD5Hash,
             lastModified: LastModified): FileData = FileData(
    hashes = Map(hashType -> hash),
    lastModified = lastModified
  )
}
