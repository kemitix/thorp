package net.kemitix.thorp.storage.api

import java.nio.file.Path

import cats.effect.IO
import net.kemitix.thorp.domain.{Logger, MD5Hash}

/**
  * Creates one, or more, hashes for local objects.
  */
trait HashService {

  def hashLocalObject(path: Path)
                     (implicit l: Logger): IO[Map[String, MD5Hash]]

}
