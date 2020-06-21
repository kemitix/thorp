package net.kemitix.thorp.lib

import java.io.File
import java.nio.file.Path

import scala.jdk.CollectionConverters._
import net.kemitix.eip.zio.MessageChannel.{EChannel, ESender}
import net.kemitix.eip.zio.{Message, MessageChannel}
import net.kemitix.thorp.config.Configuration
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
    ESender[Clock with FileScanner, Throwable, ScannedFile]
  type ScannerChannel = EChannel[Any, Throwable, ScannedFile]
  type CacheData      = (Path, FileData)
  type CacheChannel   = EChannel[Any, Throwable, CacheData]
  type CacheSender =
    ESender[Clock with FileScanner, Throwable, CacheData]

  final def scanSources(
      configuration: Configuration): RIO[FileScanner, FileSender] =
    ZIO.accessM(_.fileScanner.scanSources(configuration))

  trait Service {
    def scanSources(configuration: Configuration): RIO[FileScanner, FileSender]
  }

  trait Live extends FileScanner {
    val fileScanner: Service = new Service {

      override def scanSources(
          configuration: Configuration): RIO[FileScanner, FileSender] =
        RIO {
          fileChannel: EChannel[Clock with FileScanner,
                                Throwable,
                                ScannedFile] =>
            {
              val sources = configuration.sources
              (for {
                _ <- ZIO.foreach(sources.paths.asScala) { sourcePath =>
                  for {
                    cacheSender <- scanSource(configuration, fileChannel)(
                      sourcePath)
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
        }

      private def scanSource(configuration: Configuration,
                             fileChannel: ScannerChannel)(
          sourcePath: Path): RIO[FileScanner, CacheSender] =
        RIO { cacheChannel =>
          (for {
            cache <- UIO(FileSystem.findCache(sourcePath))
            _ <- scanPath(configuration, fileChannel, cacheChannel)(sourcePath,
                                                                    cache)
          } yield ()) <* MessageChannel.endChannel(cacheChannel)
        }

      private def scanPath(configuration: Configuration,
                           fileChannel: ScannerChannel,
                           cacheChannel: CacheChannel)(
          path: Path,
          cache: PathCache): ZIO[Clock with FileScanner, Throwable, Unit] =
        for {
          dirs <- UIO(FileSystem.listDirs(path))
          _ <- ZIO.foreach(dirs.asScala)(
            scanPath(configuration, fileChannel, cacheChannel)(_, cache))
          files = FileSystem.listFiles(path).asScala.toList
          _ <- handleFiles(configuration,
                           fileChannel,
                           cacheChannel,
                           cache,
                           files)
        } yield ()

      private def handleFiles(
          configuration: Configuration,
          fileChannel: ScannerChannel,
          cacheChannel: CacheChannel,
          pathCache: PathCache,
          files: List[File]
      ): ZIO[Clock, Throwable, List[Unit]] =
        ZIO.foreach(files) {
          handleFile(configuration, fileChannel, cacheChannel, pathCache)
        }

      private def handleFile(
          configuration: Configuration,
          fileChannel: ScannerChannel,
          cacheChannel: CacheChannel,
          cache: PathCache
      )(file: File): ZIO[Clock, Throwable, Unit] =
        for {
          isIncluded <- Filters.isIncluded(configuration, file)
          _ <- ZIO.when(isIncluded) {
            sendHashedFile(configuration, fileChannel, cacheChannel)(file,
                                                                     cache)
          }
        } yield ()

      private def sendHashedFile(
          configuration: Configuration,
          fileChannel: ScannerChannel,
          cacheChannel: CacheChannel
      )(file: File, pathCache: PathCache) = {
        val sources   = configuration.sources
        val source    = sources.forPath(file.toPath)
        val prefix    = configuration.prefix
        val path      = source.relativize(file.toPath)
        val hashes    = HashGenerator.hashObject(file.toPath)
        val remoteKey = RemoteKey.from(source, prefix, file)
        val size      = file.length()
        for {
          fileMsg <- Message.create(
            LocalFile.create(file, source.toFile, hashes, remoteKey, size))
          _        <- MessageChannel.send(fileChannel)(fileMsg)
          modified <- UIO(FileSystem.lastModified(file))
          cacheMsg <- Message.create(
            path -> FileData.create(hashes, LastModified.at(modified)))
          _ <- MessageChannel.send(cacheChannel)(cacheMsg)
        } yield ()
      }

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
