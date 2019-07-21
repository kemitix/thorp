package net.kemitix.thorp.core

import java.nio.file.Path

import net.kemitix.thorp.domain.MD5Hash
import net.kemitix.thorp.storage.api.HashService
import zio.Task

case class DummyHashService(hashes: Map[Path, Map[String, MD5Hash]])
    extends HashService {

  override def hashLocalObject(path: Path): Task[Map[String, MD5Hash]] =
    Task(hashes(path))

}
