package net.kemitix.thorp.core

import java.nio.file.Path

import net.kemitix.thorp.domain.HashType.MD5
import net.kemitix.thorp.domain.{HashType, MD5Hash}
import net.kemitix.thorp.storage.api.HashService
import zio.Task

case class SimpleHashService() extends HashService {

  override def hashLocalObject(
      path: Path
  ): Task[Map[HashType, MD5Hash]] =
    for {
      md5 <- MD5HashGenerator.md5File(path)
    } yield Map(MD5 -> md5)

}
