package net.kemitix.thorp.lib

import java.io.File
import java.nio.file.Path

import net.kemitix.eip.zio.MessageChannel.{EChannel, ESender}
import net.kemitix.eip.zio.{Message, MessageChannel}
import net.kemitix.thorp.config.Config
import net.kemitix.thorp.domain._
import net.kemitix.thorp.filesystem.{
  FileData,
  FileName,
  FileSystem,
  Hasher,
  PathCache
}
import zio.clock.Clock
import zio.{RIO, UIO, ZIO}

trait FileScanner {
  val fileScanner: FileScanner.Service
}

object FileScanner {

  type RemoteHashes = Map[MD5Hash, RemoteKey]
  type ScannedFile  = LocalFile
  type FileSender =
    ESender[Clock with Hasher with FileSystem with Config with FileScanner,
            Throwable,
            ScannedFile]
  type ScannerChannel = EChannel[Any, Throwable, ScannedFile]
  type CacheData      = (FileName, FileData)
  type CacheChannel   = EChannel[Any, Throwable, CacheData]
  type CacheSender =
    ESender[Clock with FileSystem with Hasher with Config, Throwable, CacheData]

  trait Service {
    def scanSources: RIO[FileScanner, FileSender]
  }

  trait Live extends FileScanner {
    val fileScanner: Service = new Service {

      override def scanSources: RIO[FileScanner, FileSender] =
        RIO { channel =>
          (for {
            sources <- Config.sources
            _       <- ZIO.foreach(sources.paths)(scanPath(channel)(_))
          } yield ()) <* MessageChannel.endChannel(channel)
        }

      def cacheReceiver(
          path: Path): UIO[MessageChannel.UReceiver[FileSystem, CacheData]] =
        UIO { message =>
          val (fileName, fileData) = message.body
          for {
            line <- PathCache.create(fileName, fileData)
            _    <- FileSystem.appendLines(line, PathCache.tempFileName)
          } yield ()
        }

      /**
        * Scan the files and directories for the path.
        *
        * <p>Descends into sub-directories before fetching the local cache to minimise the
        * time the cache needs to be held on the Heap. So the cache is only loaded and held
        * while 'backing-out' of the recursive calls.</p>
        *
        * @param scannerChannel Where to send selected files
        * @param path The path to scan
        */
      private def scanPath(scannerChannel: ScannerChannel)(path: Path)
        : ZIO[Clock with FileSystem with Hasher with FileScanner with Config,
              Throwable,
              Unit] =
        for {
          dirs          <- FileSystem.listDirs(path)
          _             <- ZIO.foreach(dirs)(scanPath(scannerChannel))
          files         <- FileSystem.listFiles(path)
          cache         <- FileSystem.findCache(path)
          fileHandler   <- handleFiles(scannerChannel, cache, files)
          cacheReceiver <- cacheReceiver(path)
          _ <- MessageChannel
            .pointToPoint(fileHandler)(cacheReceiver)
            .runDrain
          _ <- FileSystem.moveFile(path.resolve(PathCache.tempFileName),
                                   path.resolve(PathCache.fileName))
        } yield ()

      private def handleFiles(
          scannerChannel: ScannerChannel,
          pathCache: PathCache,
          files: List[File]
      ): RIO[FileScanner, CacheSender] =
        RIO { cacheChannel =>
          (for {
            _ <- ZIO.foreach(files) {
              handleFile(scannerChannel, cacheChannel, pathCache)
            }
          } yield ()) <* MessageChannel.endChannel(cacheChannel)
        }

      private def handleFile(
          scannerChannel: ScannerChannel,
          cacheChannel: CacheChannel,
          cache: PathCache
      )(file: File)
        : ZIO[Clock with FileSystem with Hasher with Config, Throwable, Unit] =
        for {
          isIncluded <- Filters.isIncluded(file)
          _ <- ZIO.when(isIncluded) {
            sendHashedFile(scannerChannel, cacheChannel)(file, cache)
          }
        } yield ()

      private def sendHashedFile(
          scannerChannel: ScannerChannel,
          cacheChannel: CacheChannel
      )(file: File, pathCache: PathCache) =
        for {
          sources      <- Config.sources
          source       <- Sources.forPath(file.toPath)(sources)
          prefix       <- Config.prefix
          hashes       <- Hasher.hashObject(file.toPath, pathCache.get(file))
          remoteKey    <- RemoteKey.from(source, prefix, file)
          size         <- FileSystem.length(file)
          lastModified <- FileSystem.lastModified(file)
          fileData     <- UIO(FileData.create(hashes, lastModified))
          fileName     <- UIO(file.getName)
          localFile <- ZIO(
            LocalFile(file, source.toFile, hashes, remoteKey, size))
          hashedFile <- Message.create(localFile)
          _          <- MessageChannel.send(scannerChannel)(hashedFile)
          cacheData  <- Message.create((fileName, fileData))
          _          <- MessageChannel.send(cacheChannel)(cacheData)
        } yield ()
    }

  }
  object Live extends Live
  final def scanSources: RIO[FileScanner, FileSender] =
    ZIO.accessM(_.fileScanner.scanSources)
}
