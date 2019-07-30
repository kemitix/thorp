package net.kemitix.thorp.filesystem

import java.io.{File, FileInputStream}
import java.nio.file.{Files, Path}
import java.util.stream

import zio.{Task, TaskR, UIO, ZIO, ZManaged}

import scala.collection.JavaConverters._

trait FileSystem {
  val filesystem: FileSystem.Service
}

object FileSystem {

  trait Service {
    def fileExists(file: File): ZIO[FileSystem, Throwable, Boolean]
    def openManagedFileInputStream(file: File, offset: Long = 0L)
      : TaskR[FileSystem, ZManaged[Any, Throwable, FileInputStream]]
    def fileLines(file: File): TaskR[FileSystem, List[String]]
  }
  trait Live extends FileSystem {
    override val filesystem: Service = new Service {
      override def fileExists(
          file: File
      ): ZIO[FileSystem, Throwable, Boolean] = ZIO(file.exists)

      override def openManagedFileInputStream(file: File, offset: Long)
        : TaskR[FileSystem, ZManaged[Any, Throwable, FileInputStream]] = {

        def acquire =
          Task {
            val stream = new FileInputStream(file)
            stream skip offset
            stream
          }

        def release(fis: FileInputStream) =
          UIO(fis.close())

        ZIO(ZManaged.make(acquire)(release))
      }

      override def fileLines(file: File): TaskR[FileSystem, List[String]] = {

        def acquire = ZIO(Files.lines(file.toPath))

        def release(lines: stream.Stream[String]) =
          ZIO.effectTotal(lines.close())

        def use(lines: stream.Stream[String]) =
          ZIO.effectTotal(lines.iterator.asScala.toList)

        ZIO.bracket(acquire)(release)(use)
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
        : TaskR[FileSystem, ZManaged[Any, Throwable, FileInputStream]] =
        managedFileInputStream

      override def fileLines(file: File): TaskR[FileSystem, List[String]] =
        fileLinesResult
    }
  }

  final def exists(file: File): ZIO[FileSystem, Throwable, Boolean] =
    ZIO.accessM(_.filesystem fileExists file)

  final def open(file: File, offset: Long = 0)
    : TaskR[FileSystem, ZManaged[FileSystem, Throwable, FileInputStream]] =
    ZIO.accessM(_.filesystem openManagedFileInputStream (file, offset))

  final def lines(file: File): TaskR[FileSystem, List[String]] =
    ZIO.accessM(_.filesystem fileLines (file))
}
