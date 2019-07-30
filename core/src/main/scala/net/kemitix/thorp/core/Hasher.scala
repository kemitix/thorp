package net.kemitix.thorp.core

import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference

import net.kemitix.thorp.domain.HashType.MD5
import net.kemitix.thorp.domain.{HashType, MD5Hash}
import net.kemitix.thorp.filesystem.FileSystem
import zio.{TaskR, ZIO}

/**
  * Creates one, or more, hashes for local objects.
  */
trait Hasher {
  val hasher: Hasher.Service
}
object Hasher {
  trait Service {
    def hashObject(
        path: Path): TaskR[Hasher with FileSystem, Map[HashType, MD5Hash]]
  }
  trait Live extends Hasher {
    val hasher: Service = new Service {
      override def hashObject(
          path: Path): TaskR[FileSystem, Map[HashType, MD5Hash]] =
        for {
          md5 <- MD5HashGenerator.md5File(path)
        } yield Map(MD5 -> md5)
    }
  }
  object Live extends Live

  trait Test extends Hasher {
    val hashes: AtomicReference[Map[Path, Map[HashType, MD5Hash]]] =
      new AtomicReference(Map.empty)
    val hasher: Service = new Service {
      override def hashObject(
          path: Path): TaskR[Hasher with FileSystem, Map[HashType, MD5Hash]] =
        ZIO(hashes.get()(path))
    }
  }
  object Test extends Test

  final def hashObject(
      path: Path): TaskR[Hasher with FileSystem, Map[HashType, MD5Hash]] =
    ZIO.accessM(_.hasher hashObject path)
}
