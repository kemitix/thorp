package net.kemitix.thorp.storage.aws

import java.nio.file.Path

import net.kemitix.thorp.core.Hasher
import net.kemitix.thorp.core.Hasher.Live.{hasher => CoreHasher}
import net.kemitix.thorp.core.Hasher.Service
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
          path: Path): TaskR[Hasher with FileSystem, Map[HashType, MD5Hash]] =
        for {
          base <- CoreHasher.hashObject(path)
          etag <- ETagGenerator.eTag(path).map(MD5Hash(_))
        } yield base + (ETag -> etag)

      override def hashObjectChunk(path: Path,
                                   chunkNumber: Long,
                                   chunkSize: Long)
        : TaskR[Hasher with FileSystem, Map[HashType, MD5Hash]] =
        CoreHasher.hashObjectChunk(path, chunkNumber, chunkSize)
    }

  }
}
