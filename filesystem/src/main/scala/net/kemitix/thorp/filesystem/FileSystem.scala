package net.kemitix.thorp.filesystem

import java.io.{File, FileInputStream}
import java.nio.file.{Files, Path}
import java.util.stream

import zio.{Task, RIO, UIO, ZIO, ZManaged}

import scala.collection.JavaConverters._

trait FileSystem {
  val filesystem: FileSystem.Service
}

object FileSystem {

  trait Service {
    def fileExists(file: File): ZIO[FileSystem, Throwable, Boolean]
    def openManagedFileInputStream(file: File, offset: Long)
      : RIO[FileSystem, ZManaged[Any, Throwable, FileInputStream]]
    def openManagedFileInputStream(file: File)
      : RIO[FileSystem, ZManaged[Any, Throwable, FileInputStream]] =
      openManagedFileInputStream(file, 0L)
    def fileLines(file: File): RIO[FileSystem, Seq[String]]
  }
  trait Live extends FileSystem {
    override val filesystem: Service = new Service {
      override def fileExists(
          file: File
      ): ZIO[FileSystem, Throwable, Boolean] = ZIO(file.exists)

      override def openManagedFileInputStream(file: File, offset: Long)
        : RIO[FileSystem, ZManaged[Any, Throwable, FileInputStream]] = {

        def acquire =
          Task {
            val stream = new FileInputStream(file)
            val l      = stream.skip(offset)
            stream
          }

        def release(fis: FileInputStream) =
          UIO(fis.close())

        ZIO(ZManaged.make(acquire)(release))
      }

      override def fileLines(file: File): RIO[FileSystem, Seq[String]] = {
        def acquire = ZIO(Files.lines(file.toPath))
        def use(lines: stream.Stream[String]) =
          ZIO.effectTotal(lines.iterator.asScala.toList)
        acquire.bracketAuto(use)
      }
    }
  }
  object Live extends Live
  trait Test extends FileSystem {

    val fileExistsResultMap: Task[Map[Path, File]]
    val fileLinesResult: Task[List[String]]
    val managedFileInputStream: Task[ZManaged[Any, Throwable, FileInputStream]]

    override val filesystem: Service = new Service {

      override def fileExists(file: File): ZIO[FileSystem, Throwable, Boolean] =
        fileExistsResultMap.map(m => m.keys.exists(_ equals file.toPath))

      override def openManagedFileInputStream(file: File, offset: Long)
        : RIO[FileSystem, ZManaged[Any, Throwable, FileInputStream]] =
        managedFileInputStream

      override def fileLines(file: File): RIO[FileSystem, List[String]] =
        fileLinesResult
    }
  }

  final def exists(file: File): ZIO[FileSystem, Throwable, Boolean] =
    ZIO.accessM(_.filesystem fileExists file)

  final def open(file: File, offset: Long)
    : RIO[FileSystem, ZManaged[FileSystem, Throwable, FileInputStream]] =
    ZIO.accessM(_.filesystem openManagedFileInputStream (file, offset))

  final def open(file: File)
    : RIO[FileSystem, ZManaged[FileSystem, Throwable, FileInputStream]] =
    open(file, 0L)

  final def lines(file: File): RIO[FileSystem, Seq[String]] =
    ZIO.accessM(_.filesystem fileLines (file))
}
