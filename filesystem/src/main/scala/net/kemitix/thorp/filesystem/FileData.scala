package net.kemitix.thorp.filesystem

import net.kemitix.thorp.domain.{Hashes, LastModified}

case class FileData(
    hashes: Hashes,
    lastModified: LastModified
) {}

object FileData {
  def create(hashes: Hashes, lastModified: LastModified): FileData = FileData(
    hashes = hashes,
    lastModified = lastModified
  )
}
