package net.kemitix.thorp.storage.aws

import java.nio.file.Path

import net.kemitix.thorp.core.Hasher.Service
import net.kemitix.thorp.core.{Hasher, MD5HashGenerator}
import net.kemitix.thorp.domain.HashType.MD5
import net.kemitix.thorp.domain.{HashType, MD5Hash}
import net.kemitix.thorp.filesystem.FileSystem
import zio.TaskR

object S3Hasher {

  trait Live extends Hasher {
    val hasher: Service = new Service {

      /**
        * Generates an MD5 Hash and an multi-part ETag
        *
        * @param path the local path to scan
        * @return a set of hash values
        */
      override def hashObject(
          path: Path): TaskR[FileSystem, Map[HashType, MD5Hash]] =
        for {
          md5  <- MD5HashGenerator.md5File(path)
          etag <- ETagGenerator.eTag(path).map(MD5Hash(_))
        } yield
          Map(
            MD5  -> md5,
            ETag -> etag
          )
    }

  }
}
