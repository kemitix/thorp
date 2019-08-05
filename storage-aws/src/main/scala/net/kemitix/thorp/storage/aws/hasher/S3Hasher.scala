package net.kemitix.thorp.storage.aws.hasher

import java.nio.file.Path

import net.kemitix.thorp.core.hasher.Hasher
import net.kemitix.thorp.core.hasher.Hasher.Live.{hasher => CoreHasher}
import net.kemitix.thorp.core.hasher.Hasher.Service
import net.kemitix.thorp.domain.{HashType, MD5Hash}
import net.kemitix.thorp.filesystem.FileSystem
import net.kemitix.thorp.storage.aws.ETag
import zio.RIO

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
          path: Path): RIO[Hasher with FileSystem, Map[HashType, MD5Hash]] =
        for {
          base <- CoreHasher.hashObject(path)
          etag <- ETagGenerator.eTag(path).map(MD5Hash(_))
        } yield base + (ETag -> etag)

      override def hashObjectChunk(path: Path,
                                   chunkNumber: Long,
                                   chunkSize: Long)
        : RIO[Hasher with FileSystem, Map[HashType, MD5Hash]] =
        CoreHasher.hashObjectChunk(path, chunkNumber, chunkSize)

      override def hex(in: Array[Byte]): RIO[Hasher, String] =
        CoreHasher.hex(in)

      override def digest(in: String): RIO[Hasher, Array[Byte]] =
        CoreHasher.digest(in)
    }

  }
}
