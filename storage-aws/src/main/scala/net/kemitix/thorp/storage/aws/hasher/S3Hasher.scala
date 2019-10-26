package net.kemitix.thorp.storage.aws.hasher

import java.nio.file.Path

import net.kemitix.thorp.domain.{HashType, Hashes, MD5Hash}
import net.kemitix.thorp.filesystem.Hasher.Live.{hasher => CoreHasher}
import net.kemitix.thorp.filesystem.Hasher.Service
import net.kemitix.thorp.filesystem.{FileData, FileSystem, Hasher}
import net.kemitix.thorp.storage.aws.ETag
import zio.{RIO, ZIO}

object S3Hasher {

  trait Live extends Hasher {
    val hasher: Service = new Service {

      /**
        * Generates an MD5 Hash and an multi-part ETag
        *
        * @param path the local path to scan
        * @return a set of hash values
        */
      override def hashObject(path: Path, cachedFileData: Option[FileData])
        : RIO[Hasher with FileSystem, Hashes] =
        for {
          base <- CoreHasher.hashObject(path, cachedFileData)
          etag <- ETagGenerator.eTag(path).map(MD5Hash(_))
        } yield base + (ETag -> etag)

      override def hashObjectChunk(
          path: Path,
          chunkNumber: Long,
          chunkSize: Long): RIO[Hasher with FileSystem, Hashes] =
        CoreHasher.hashObjectChunk(path, chunkNumber, chunkSize)

      override def hex(in: Array[Byte]): RIO[Hasher, String] =
        CoreHasher.hex(in)

      override def digest(in: String): RIO[Hasher, Array[Byte]] =
        CoreHasher.digest(in)

      override def typeFrom(
          str: String): ZIO[Hasher, IllegalArgumentException, HashType] =
        if (str.contentEquals("ETag")) {
          RIO.succeed(ETag)
        } else {
          CoreHasher.typeFrom(str)
        }

    }

  }
}
