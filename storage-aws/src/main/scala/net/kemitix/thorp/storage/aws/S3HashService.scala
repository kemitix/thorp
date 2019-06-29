package net.kemitix.thorp.storage.aws

import java.io.File

import cats.effect.IO
import net.kemitix.thorp.core.MD5HashGenerator
import net.kemitix.thorp.domain.{Logger, MD5Hash}
import net.kemitix.thorp.storage.api.HashService

trait S3HashService extends HashService {

  /**
    * Generates an MD5 Hash and an multi-part ETag
    *
    * @param file the local file to scan
    * @return a set of hash values
    */
  override def hashLocalObject(file: File)(implicit l: Logger): IO[Map[String, MD5Hash]] =
    for {
      md5 <- MD5HashGenerator.md5File(file)
      etag <- ETagGenerator.eTag(file).map(MD5Hash(_))
    } yield Map(
      "md5" -> md5,
      "etag" -> etag
    )

}

object S3HashService extends S3HashService {
  lazy val defaultHashService: HashService = S3HashService
}
