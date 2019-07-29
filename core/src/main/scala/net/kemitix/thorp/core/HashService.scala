package net.kemitix.thorp.core

import java.nio.file.Path

import net.kemitix.thorp.domain.{HashType, MD5Hash}
import net.kemitix.thorp.filesystem.FileSystem
import zio.TaskR

/**
  * Creates one, or more, hashes for local objects.
  */
trait HashService {

  def hashLocalObject(path: Path): TaskR[FileSystem, Map[HashType, MD5Hash]]

}
