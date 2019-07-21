package net.kemitix.thorp.storage.api

import java.nio.file.Path

import net.kemitix.thorp.domain.MD5Hash
import zio.Task

/**
  * Creates one, or more, hashes for local objects.
  */
trait HashService {

  def hashLocalObject(path: Path): Task[Map[String, MD5Hash]]

}
