package net.kemitix.thorp.filesystem

import java.io.{File, FileInputStream}
import java.nio.file.{Files, Path}
import java.time.Instant
import java.util.stream

import net.kemitix.thorp.domain.{Hashes, RemoteKey, Sources}
import zio._

import scala.jdk.CollectionConverters._

trait FileSystem {
  val filesystem: FileSystem.Service
}

object FileSystem {
  trait Service {
    def fileExists(file: File): ZIO[FileSystem, Nothing, Boolean]
    def openManagedFileInputStream(file: File, offset: Long)
      : RIO[FileSystem, ZManaged[Any, Throwable, FileInputStream]]
    def fileLines(file: File): RIO[FileSystem, Seq[String]]
    def isDirectory(file: File): RIO[FileSystem, Boolean]
    def listFiles(path: Path): UIO[List[File]]
    def length(file: File): ZIO[FileSystem, Nothing, Long]
    def hasLocalFile(sources: Sources,
                     prefix: RemoteKey,
                     remoteKey: RemoteKey): ZIO[FileSystem, Nothing, Boolean]
    def findCache(directory: Path): ZIO[FileSystem, Nothing, PathCache]
    def getHashes(path: Path, fileData: FileData): ZIO[FileSystem, Any, Hashes]
  }
  trait Live extends FileSystem {
    override val filesystem: Service = new Service {
      override def fileExists(
          file: File
      ): ZIO[FileSystem, Nothing, Boolean] = UIO(file.exists)

      override def openManagedFileInputStream(file: File, offset: Long)
        : RIO[FileSystem, ZManaged[Any, Throwable, FileInputStream]] = {

        def acquire =
          Task {
            val stream = new FileInputStream(file)
            val _      = stream.skip(offset)
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

      override def isDirectory(file: File): RIO[FileSystem, Boolean] =
        Task(file.isDirectory)

      override def listFiles(path: Path): UIO[List[File]] =
        Task(List.from(path.toFile.listFiles()))
          .catchAll(_ => UIO.succeed(List.empty[File]))

      override def length(file: File): ZIO[FileSystem, Nothing, Long] =
        UIO(file.length)

      override def hasLocalFile(
          sources: Sources,
          prefix: RemoteKey,
          remoteKey: RemoteKey): ZIO[FileSystem, Nothing, Boolean] = {
        ZIO.foldLeft(sources.paths)(false) { (accExists, source) =>
          RemoteKey
            .asFile(source, prefix)(remoteKey)
            .map(FileSystem.exists)
            .getOrElse(UIO(false))
            .map(_ || accExists)
        }
      }

      override def findCache(
          directory: Path): ZIO[FileSystem, Nothing, PathCache] =
        for {
          cacheFile <- UIO(directory.resolve(".thorp.cache").toFile)
          lines     <- fileLines(cacheFile).catchAll(_ => UIO(List.empty))
          cache     <- PathCache.fromLines(lines)
        } yield cache

      override def getHashes(
          path: Path,
          fileData: FileData): ZIO[FileSystem, Any, Hashes] = {
        val lastModified = Instant.ofEpochMilli(path.toFile.lastModified())
        if (lastModified.isAfter(fileData.lastModified)) {
          ZIO.fail("fileData is out-of-date")
        } else {
          ZIO.succeed(fileData.hashes)
        }
      }
    }
  }
  object Live extends Live
  trait Test extends FileSystem {

    val fileExistsResultMap: UIO[Map[Path, File]]
    val fileLinesResult: Task[List[String]]
    val isDirResult: Task[Boolean]
    val listFilesResult: UIO[List[File]]
    val lengthResult: UIO[Long]
    val managedFileInputStream: Task[ZManaged[Any, Throwable, FileInputStream]]
    val hasLocalFileResult: UIO[Boolean]
    val pathCacheResult: UIO[PathCache]
    val matchesResult: IO[Any, Hashes]

    override val filesystem: Service = new Service {

      override def fileExists(file: File): ZIO[FileSystem, Nothing, Boolean] =
        fileExistsResultMap.map(m => m.keys.exists(_ equals file.toPath))

      override def openManagedFileInputStream(file: File, offset: Long)
        : RIO[FileSystem, ZManaged[Any, Throwable, FileInputStream]] =
        managedFileInputStream

      override def fileLines(file: File): RIO[FileSystem, List[String]] =
        fileLinesResult

      override def isDirectory(file: File): RIO[FileSystem, Boolean] =
        isDirResult

      override def listFiles(path: Path): UIO[List[File]] =
        listFilesResult

      override def length(file: File): UIO[Long] =
        lengthResult

      override def hasLocalFile(
          sources: Sources,
          prefix: RemoteKey,
          remoteKey: RemoteKey): ZIO[FileSystem, Nothing, Boolean] =
        hasLocalFileResult

      override def findCache(directory: Path): UIO[PathCache] =
        pathCacheResult

      override def getHashes(path: Path,
                             fileData: FileData): ZIO[FileSystem, Any, Hashes] =
        matchesResult
    }
  }

  final def exists(file: File): ZIO[FileSystem, Nothing, Boolean] =
    ZIO.accessM(_.filesystem fileExists file)

  final def openAtOffset(file: File, offset: Long)
    : RIO[FileSystem, ZManaged[FileSystem, Throwable, FileInputStream]] =
    ZIO.accessM(_.filesystem openManagedFileInputStream (file, offset))

  final def open(file: File)
    : RIO[FileSystem, ZManaged[FileSystem, Throwable, FileInputStream]] =
    ZIO.accessM(_.filesystem openManagedFileInputStream (file, 0L))

  final def lines(file: File): RIO[FileSystem, Seq[String]] =
    ZIO.accessM(_.filesystem fileLines (file))

  final def isDirectory(file: File): RIO[FileSystem, Boolean] =
    ZIO.accessM(_.filesystem.isDirectory(file))

  final def listFiles(path: Path): ZIO[FileSystem, Nothing, List[File]] =
    ZIO.accessM(_.filesystem.listFiles(path))

  final def length(file: File): ZIO[FileSystem, Nothing, Long] =
    ZIO.accessM(_.filesystem.length(file))

  final def hasLocalFile(
      sources: Sources,
      prefix: RemoteKey,
      remoteKey: RemoteKey): ZIO[FileSystem, Nothing, Boolean] =
    ZIO.accessM(_.filesystem.hasLocalFile(sources, prefix, remoteKey))

  final def findCache(directory: Path): ZIO[FileSystem, Nothing, PathCache] =
    ZIO.accessM(_.filesystem.findCache(directory))

  final def getHashes(path: Path,
                      fileData: FileData): ZIO[FileSystem, Any, Hashes] =
    ZIO.accessM(_.filesystem.getHashes(path, fileData))
}
