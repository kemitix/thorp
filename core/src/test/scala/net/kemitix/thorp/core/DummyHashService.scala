package net.kemitix.thorp.core

import java.io.File

import cats.effect.IO
import net.kemitix.thorp.domain.{Logger, MD5Hash}
import net.kemitix.thorp.storage.api.HashService

case class DummyHashService(hashes: Map[File, Map[String, MD5Hash]]) extends HashService {
  override def hashLocalObject(file: File)(implicit l: Logger): IO[Map[String, MD5Hash]] = IO.pure(hashes(file))
}
