package net.kemitix.thorp.lib

import java.io.File
import java.nio.file.Path
import scala.jdk.CollectionConverters._

import net.kemitix.eip.zio.MessageChannel.{EChannel, ESender}
import net.kemitix.eip.zio.{Message, MessageChannel}
import net.kemitix.thorp.config.Config
import net.kemitix.thorp.domain._
import net.kemitix.thorp.filesystem._
import zio.clock.Clock
import zio.{RIO, UIO, ZIO}

trait FileScanner {
  val fileScanner: FileScanner.Service
}

object FileScanner {

  type RemoteHashes = Map[MD5Hash, RemoteKey]
  type ScannedFile  = LocalFile
  type FileSender =
    ESender[Clock with Config with FileScanner, Throwable, ScannedFile]
  type ScannerChannel = EChannel[Any, Throwable, ScannedFile]
  type CacheData      = (Path, FileData)
  type CacheChannel   = EChannel[Any, Throwable, CacheData]
  type CacheSender =
    ESender[Clock with FileScanner with Config, Throwable, CacheData]

  final def scanSources: RIO[FileScanner, FileSender] =
    ZIO.accessM(_.fileScanner.scanSources)

  trait Service {
    def scanSources: RIO[FileScanner, FileSender]
  }

  trait Live extends FileScanner {
    val fileScanner: Service = new Service {

      override def scanSources: RIO[FileScanner, FileSender] =
        RIO {
          fileChannel: EChannel[Clock with Config with FileScanner,
                                Throwable,
                                ScannedFile] =>
            (for {
              sources <- Config.sources
              _ <- ZIO.foreach(sources.paths.asScala) { sourcePath =>
                for {
                  cacheSender   <- scanSource(fileChannel)(sourcePath)
                  cacheReceiver <- cacheReceiver(sourcePath)
                  _ <- MessageChannel
                    .pointToPoint(cacheSender)(cacheReceiver)
                    .runDrain
                  _ = FileSystem.moveFile(
                    sourcePath.resolve(PathCache.tempFileName),
                    sourcePath.resolve(PathCache.fileName))
                } yield ()
              }
            } yield ()) <* MessageChannel.endChannel(fileChannel)
        }

      private def scanSource(fileChannel: ScannerChannel)(
          sourcePath: Path): RIO[FileScanner, CacheSender] =
        RIO { cacheChannel =>
          (for {
            cache <- UIO(FileSystem.findCache(sourcePath))
            _     <- scanPath(fileChannel, cacheChannel)(sourcePath, cache)
          } yield ()) <* MessageChannel.endChannel(cacheChannel)
        }

      private def scanPath(
          fileChannel: ScannerChannel,
          cacheChannel: CacheChannel)(path: Path, cache: PathCache)
        : ZIO[Clock with FileScanner with Config, Throwable, Unit] =
        for {
          dirs <- UIO(FileSystem.listDirs(path))
          _ <- ZIO.foreach(dirs.asScala)(
            scanPath(fileChannel, cacheChannel)(_, cache))
          files = FileSystem.listFiles(path).asScala.toList
          _ <- handleFiles(fileChannel, cacheChannel, cache, files)
        } yield ()

      private def handleFiles(
          fileChannel: ScannerChannel,
          cacheChannel: CacheChannel,
          pathCache: PathCache,
          files: List[File]
      ): ZIO[Clock with Config, Throwable, List[Unit]] =
        ZIO.foreach(files) {
          handleFile(fileChannel, cacheChannel, pathCache)
        }

      private def handleFile(
          fileChannel: ScannerChannel,
          cacheChannel: CacheChannel,
          cache: PathCache
      )(file: File): ZIO[Clock with Config, Throwable, Unit] =
        for {
          isIncluded <- Filters.isIncluded(file)
          _ <- ZIO.when(isIncluded) {
            sendHashedFile(fileChannel, cacheChannel)(file, cache)
          }
        } yield ()

      private def sendHashedFile(
          fileChannel: ScannerChannel,
          cacheChannel: CacheChannel
      )(file: File, pathCache: PathCache) =
        for {
          sources <- Config.sources
          source = sources.forPath(file.toPath)
          prefix <- Config.prefix
          path      = source.relativize(file.toPath)
          hashes    = HashGenerator.hashObject(file.toPath)
          remoteKey = RemoteKey.from(source, prefix, file)
          size      = file.length()
          fileMsg <- Message.create(
            LocalFile.create(file, source.toFile, hashes, remoteKey, size))
          _        <- MessageChannel.send(fileChannel)(fileMsg)
          modified <- UIO(FileSystem.lastModified(file))
          cacheMsg <- Message.create(
            path -> FileData.create(hashes, LastModified.at(modified)))
          _ <- MessageChannel.send(cacheChannel)(cacheMsg)
        } yield ()

      def cacheReceiver(
          sourcePath: Path): UIO[MessageChannel.UReceiver[Any, CacheData]] = {
        val tempFile = sourcePath.resolve(PathCache.tempFileName).toFile
        UIO { message =>
          val (path, fileData) = message.body
          for {
            line <- UIO(PathCache.export(path, fileData).asScala)
            _    <- UIO(FileSystem.appendLines(line.toList.asJava, tempFile))
          } yield ()
        }
      }
    }

  }

  object Live extends Live
}
