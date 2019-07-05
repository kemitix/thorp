package net.kemitix.thorp.core

import java.nio.file.Path

import cats.effect.IO
import net.kemitix.thorp.domain.{Logger, MD5Hash}
import net.kemitix.thorp.storage.api.HashService

case class DummyHashService(hashes: Map[Path, Map[String, MD5Hash]])
  extends HashService {

  override def hashLocalObject(path: Path)
                              (implicit l: Logger): IO[Map[String, MD5Hash]] =
    IO.pure(hashes(path))

}
