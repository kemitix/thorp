package net.kemitix.thorp.core

import java.nio.file.Path

import net.kemitix.thorp.domain.{HashType, MD5Hash}
import net.kemitix.thorp.storage.api.HashService
import zio.Task

case class DummyHashService(hashes: Map[Path, Map[HashType, MD5Hash]])
    extends HashService {

  override def hashLocalObject(path: Path): Task[Map[HashType, MD5Hash]] =
    Task(hashes(path))

}
