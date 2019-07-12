package net.kemitix.thorp.core

import java.nio.file.Path

import cats.effect.IO
import net.kemitix.thorp.domain.{Logger, MD5Hash}
import net.kemitix.thorp.storage.api.HashService

case class SimpleHashService() extends HashService {

  override def hashLocalObject(path: Path)
                              (implicit l: Logger): IO[Map[String, MD5Hash]] =
    for {
      md5 <- MD5HashGenerator.md5File(path)
    } yield Map(
      "md5" -> md5
    )

}
