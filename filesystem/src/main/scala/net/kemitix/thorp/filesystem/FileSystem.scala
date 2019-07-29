package net.kemitix.thorp.filesystem

import java.io.{File, FileInputStream}
import java.nio.file.Path

import zio.{Task, TaskR, UIO, ZIO, ZManaged}

trait FileSystem {
  val filesystem: FileSystem.Service
}

object FileSystem {

  trait Service {
    def fileExists(file: File): ZIO[FileSystem, Throwable, Boolean]
    def openManagedFileInputStream(file: File, offset: Long = 0L)
      : TaskR[FileSystem, ZManaged[Any, Throwable, FileInputStream]]
  }
  trait Live extends FileSystem {
    override val filesystem: Service = new Service {
      override def fileExists(
          file: File
      ): ZIO[FileSystem, Throwable, Boolean] = ZIO(file.exists)

      override def openManagedFileInputStream(file: File, offset: Long)
        : TaskR[FileSystem, ZManaged[Any, Throwable, FileInputStream]] =
        ZIO {
          ZManaged.make {
            Task {
              val stream = new FileInputStream(file)
              stream skip offset
              stream
            }
          } { fis =>
            UIO(fis.close())
          }
        }
    }
  }
  object Live extends Live
  trait Test extends FileSystem {

    def fileSystem: Task[Map[Path, File]]

    override val filesystem: Service = new Service {

      override def fileExists(file: File): ZIO[FileSystem, Throwable, Boolean] =
        fileSystem.map(m => m.keys.exists(_ equals file.toPath))

      override def openManagedFileInputStream(file: File, offset: Long)
        : TaskR[FileSystem, ZManaged[Any, Throwable, FileInputStream]] = ???
    }
  }
}
