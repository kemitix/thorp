package net.kemitix.thorp.lib

import java.io.File
import java.nio.file.Path

import net.kemitix.eip.zio.MessageChannel.{EChannel, ESender}
import net.kemitix.eip.zio.{Message, MessageChannel}
import net.kemitix.thorp.config.Config
import net.kemitix.thorp.domain._
import net.kemitix.thorp.filesystem.{FileSystem, Hasher, PathCache}
import zio.clock.Clock
import zio.{RIO, ZIO}

trait FileScanner {
  val fileScanner: FileScanner.Service
}

object FileScanner {

  type RemoteHashes = Map[MD5Hash, RemoteKey]
  type ScannedFile  = LocalFile
  type FileSender = ESender[Clock with Hasher with FileSystem with Config,
                            Throwable,
                            ScannedFile]
  type ScannerChannel = EChannel[Any, Throwable, ScannedFile]

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

      /**
        * Scan the files and directories for the path.
        *
        * <p>Descends into sub-directories before fetching the local cache to minimise the
        * time the cache needs to be held on the Heap. So the cache is only loaded and held
        * while 'backing-out' of the recursive calls.</p>
        *
        * @param channel Where to send selected files
        * @param path The path to scan
        */
      private def scanPath(channel: ScannerChannel)(path: Path)
        : ZIO[Clock with Config with Hasher with FileSystem, Throwable, Unit] =
        for {
          dirs  <- FileSystem.listDirs(path)
          _     <- ZIO.foreach(dirs)(scanPath(channel))
          files <- FileSystem.listFiles(path)
          cache <- FileSystem.findCache(path)
          _     <- ZIO.foreach(files)(handleFile(channel, cache))
        } yield ()

      private def handleFile(
          channel: ScannerChannel,
          cache: PathCache
      )(file: File)
        : ZIO[Clock with FileSystem with Hasher with Config, Throwable, Unit] =
        for {
          isIncluded <- Filters.isIncluded(file)
          _          <- ZIO.when(isIncluded)(sendHashedFile(channel)(file, cache))
        } yield ()

      private def sendHashedFile(
          channel: ScannerChannel)(file: File, pathCache: PathCache) =
        for {
          sources   <- Config.sources
          source    <- Sources.forPath(file.toPath)(sources)
          prefix    <- Config.prefix
          hashes    <- Hasher.hashObject(file.toPath, pathCache.get(file))
          remoteKey <- RemoteKey.from(source, prefix, file)
          size      <- FileSystem.length(file)
          localFile <- ZIO(
            LocalFile(file, source.toFile, hashes, remoteKey, size))
          hashedFile <- Message.create(localFile)
          _          <- MessageChannel.send(channel)(hashedFile)
        } yield ()
    }

  }
  object Live extends Live
  final def scanSources: RIO[FileScanner, FileSender] =
    ZIO.accessM(_.fileScanner.scanSources)
}
