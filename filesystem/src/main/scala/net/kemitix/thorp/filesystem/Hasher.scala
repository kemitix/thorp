package net.kemitix.thorp.filesystem

import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference

import net.kemitix.thorp.domain.HashType.MD5
import net.kemitix.thorp.domain.Hashes
import zio.{RIO, ZIO}

/**
  * Creates one, or more, hashes for local objects.
  */
trait Hasher {
  val hasher: Hasher.Service
}
object Hasher {
  trait Service {
    def hashObject(path: Path): RIO[Hasher with FileSystem, Hashes]
    def hashObjectChunk(path: Path,
                        chunkNumber: Long,
                        chunkSize: Long): RIO[Hasher with FileSystem, Hashes]
    def hex(in: Array[Byte]): RIO[Hasher, String]
    def digest(in: String): RIO[Hasher, Array[Byte]]
  }
  trait Live extends Hasher {
    val hasher: Service = new Service {
      override def hashObject(path: Path): RIO[FileSystem, Hashes] =
        for {
          md5 <- MD5HashGenerator.md5File(path)
        } yield Map(MD5 -> md5)

      override def hashObjectChunk(
          path: Path,
          chunkNumber: Long,
          chunkSize: Long): RIO[Hasher with FileSystem, Hashes] =
        for {
          md5 <- MD5HashGenerator.md5FileChunk(path,
                                               chunkNumber * chunkSize,
                                               chunkSize)
        } yield Map(MD5 -> md5)

      override def hex(in: Array[Byte]): RIO[Hasher, String] =
        ZIO(MD5HashGenerator.hex(in))

      override def digest(in: String): RIO[Hasher, Array[Byte]] =
        ZIO(MD5HashGenerator.digest(in))
    }
  }
  object Live extends Live

  trait Test extends Hasher {
    val hashes: AtomicReference[Map[Path, Hashes]] =
      new AtomicReference(Map.empty)
    val hashChunks: AtomicReference[Map[Path, Map[Long, Hashes]]] =
      new AtomicReference(Map.empty)
    val hasher: Service = new Service {
      override def hashObject(path: Path): RIO[Hasher with FileSystem, Hashes] =
        ZIO(hashes.get()(path))

      override def hashObjectChunk(
          path: Path,
          chunkNumber: Long,
          chunkSize: Long): RIO[Hasher with FileSystem, Hashes] =
        ZIO(hashChunks.get()(path)(chunkNumber))

      override def hex(in: Array[Byte]): RIO[Hasher, String] =
        ZIO(MD5HashGenerator.hex(in))

      override def digest(in: String): RIO[Hasher, Array[Byte]] =
        ZIO(MD5HashGenerator.digest(in))
    }
  }
  object Test extends Test

  final def hashObject(path: Path): RIO[Hasher with FileSystem, Hashes] =
    ZIO.accessM(_.hasher hashObject path)

  final def hashObjectChunk(
      path: Path,
      chunkNumber: Long,
      chunkSize: Long): RIO[Hasher with FileSystem, Hashes] =
    ZIO.accessM(_.hasher hashObjectChunk (path, chunkNumber, chunkSize))

  final def hex(in: Array[Byte]): RIO[Hasher, String] =
    ZIO.accessM(_.hasher hex in)

  final def digest(in: String): RIO[Hasher, Array[Byte]] =
    ZIO.accessM(_.hasher digest in)
}
