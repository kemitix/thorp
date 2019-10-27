package net.kemitix.thorp.filesystem

import java.io.{File, FileInputStream, FileWriter}
import java.nio.file.{Files, Path, StandardCopyOption}
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
    def appendLines(lines: Iterable[String], file: File): UIO[Unit]
    def isDirectory(file: File): RIO[FileSystem, Boolean]
    def listFiles(path: Path): UIO[List[File]]
    def listDirs(path: Path): UIO[List[Path]]
    def length(file: File): ZIO[FileSystem, Nothing, Long]
    def lastModified(file: File): UIO[Instant]
    def hasLocalFile(sources: Sources,
                     prefix: RemoteKey,
                     remoteKey: RemoteKey): ZIO[FileSystem, Nothing, Boolean]
    def findCache(
        directory: Path): ZIO[FileSystem with Hasher, Nothing, PathCache]
    def getHashes(path: Path, fileData: FileData): ZIO[FileSystem, Any, Hashes]
    def moveFile(source: Path, target: Path): UIO[Unit]
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
        Task {
          List
            .from(path.toFile.listFiles())
            .filterNot(_.isDirectory)
            .filterNot(_.getName.contentEquals(PathCache.fileName))
            .filterNot(_.getName.contentEquals(PathCache.tempFileName))
        }.catchAll(_ => UIO.succeed(List.empty[File]))

      override def listDirs(path: Path): UIO[List[Path]] =
        Task(
          List
            .from(path.toFile.listFiles())
            .filter(_.isDirectory)
            .map(_.toPath))
          .catchAll(_ => UIO.succeed(List.empty[Path]))

      override def length(file: File): ZIO[FileSystem, Nothing, Long] =
        UIO(file.length)

      override def lastModified(file: File): UIO[Instant] =
        UIO(Instant.ofEpochMilli(file.lastModified()))

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
          directory: Path): ZIO[FileSystem with Hasher, Nothing, PathCache] =
        for {
          cacheFile <- UIO(directory.resolve(PathCache.fileName).toFile)
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

      override def appendLines(lines: Iterable[String], file: File): UIO[Unit] =
        UIO.bracket(UIO(new FileWriter(file, true)))(fw => UIO(fw.close()))(
          fw =>
            UIO {
              lines.map(line => fw.append(line + System.lineSeparator()))
          })

      override def moveFile(source: Path, target: Path): UIO[Unit] =
        UIO {
          if (source.toFile.exists()) {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
          }
          ()
        }.catchAll(_ => UIO.unit)
    }
  }
  object Live extends Live
  trait Test extends FileSystem {

    val fileExistsResultMap: UIO[Map[Path, File]]
    val fileLinesResult: Task[List[String]]
    val isDirResult: Task[Boolean]
    val listFilesResult: UIO[List[File]]
    val listDirsResult: UIO[List[Path]]
    val lengthResult: UIO[Long]
    val lastModifiedResult: UIO[Instant]
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

      override def listDirs(path: Path): UIO[List[Path]] =
        listDirsResult

      override def length(file: File): UIO[Long] =
        lengthResult

      override def lastModified(file: File): UIO[Instant] =
        lastModifiedResult

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

      override def appendLines(lines: Iterable[String], file: File): UIO[Unit] =
        UIO.unit

      override def moveFile(source: Path, target: Path): UIO[Unit] =
        UIO.unit
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

  /**
    * Lists only files within the Path.
    */
  final def listFiles(path: Path): ZIO[FileSystem, Nothing, List[File]] =
    ZIO.accessM(_.filesystem.listFiles(path))

  /**
    * Lists only sub-directories within the Path.
    */
  final def listDirs(path: Path): ZIO[FileSystem, Nothing, List[Path]] =
    ZIO.accessM(_.filesystem.listDirs(path))

  final def length(file: File): ZIO[FileSystem, Nothing, Long] =
    ZIO.accessM(_.filesystem.length(file))

  final def hasLocalFile(
      sources: Sources,
      prefix: RemoteKey,
      remoteKey: RemoteKey): ZIO[FileSystem, Nothing, Boolean] =
    ZIO.accessM(_.filesystem.hasLocalFile(sources, prefix, remoteKey))

  final def findCache(
      directory: Path): ZIO[FileSystem with Hasher, Nothing, PathCache] =
    ZIO.accessM(_.filesystem.findCache(directory))

  final def getHashes(path: Path,
                      fileData: FileData): ZIO[FileSystem, Any, Hashes] =
    ZIO.accessM(_.filesystem.getHashes(path, fileData))

  final def lastModified(file: File): ZIO[FileSystem, Nothing, Instant] =
    ZIO.accessM(_.filesystem.lastModified(file))

  final def appendLines(lines: Iterable[String],
                        file: File): ZIO[FileSystem, Nothing, Unit] =
    ZIO.accessM(_.filesystem.appendLines(lines, file))

  final def moveFile(
      source: Path,
      target: Path
  ): ZIO[FileSystem, Nothing, Unit] =
    ZIO.accessM(_.filesystem.moveFile(source, target))

}
