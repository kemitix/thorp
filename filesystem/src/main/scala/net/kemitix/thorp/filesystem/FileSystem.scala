package net.kemitix.thorp.filesystem

import java.io.File
import java.nio.file.Path

import zio.{Task, ZIO}

trait FileSystem {
  val filesystem: FileSystem.Service
}

object FileSystem {
  trait Service {
    def fileExists(file: File): ZIO[FileSystem, Throwable, Boolean]
  }
  trait Live extends FileSystem {
    override val filesystem: Service = new Service {
      override def fileExists(
          file: File
      ): ZIO[FileSystem, Throwable, Boolean] = ZIO(file.exists)
    }
  }
  trait Test extends FileSystem {

    def fileSystem: Task[Map[Path, File]]

    override val filesystem: Service = new Service {

      override def fileExists(file: File): ZIO[FileSystem, Throwable, Boolean] =
        fileSystem.map(m => m.keys.exists(_ equals file.toPath))
    }
  }
}
