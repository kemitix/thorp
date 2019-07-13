package net.kemitix.thorp.storage.aws

import java.nio.file.Path

import cats.effect.IO
import net.kemitix.thorp.core.MD5HashGenerator
import net.kemitix.thorp.domain.{Logger, MD5Hash}
import net.kemitix.thorp.storage.api.HashService

trait S3HashService extends HashService {

  /**
    * Generates an MD5 Hash and an multi-part ETag
    *
    * @param path the local path to scan
    * @return a set of hash values
    */
  override def hashLocalObject(
      path: Path
  )(implicit l: Logger): IO[Map[String, MD5Hash]] =
    for {
      md5  <- MD5HashGenerator.md5File(path)
      etag <- ETagGenerator.eTag(path).map(MD5Hash(_))
    } yield
      Map(
        "md5"  -> md5,
        "etag" -> etag
      )

}

object S3HashService extends S3HashService {
  lazy val defaultHashService: HashService = S3HashService
}
