package net.kemitix.thorp.core

import java.nio.file.Path

import net.kemitix.thorp.domain.MD5Hash
import net.kemitix.thorp.storage.api.HashService
import zio.Task

case class SimpleHashService() extends HashService {

  override def hashLocalObject(
      path: Path
  ): Task[Map[String, MD5Hash]] =
    for {
      md5 <- MD5HashGenerator.md5File(path)
    } yield Map("md5" -> md5)

}
