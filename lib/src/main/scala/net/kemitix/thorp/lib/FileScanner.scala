package net.kemitix.thorp.lib

import java.io.File
import java.nio.file.Path

import net.kemitix.eip.zio.MessageChannel.{EChannel, ESender}
import net.kemitix.eip.zio.{Message, MessageChannel}
import net.kemitix.thorp.config.Config
import net.kemitix.thorp.domain._
import net.kemitix.thorp.filesystem.{FileSystem, Hasher}
import zio.clock.Clock
import zio.{RIO, UIO, ZIO}

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

      private def scanPath(channel: ScannerChannel)(path: Path)
        : ZIO[Clock with Config with Hasher with FileSystem, Throwable, Unit] =
        for {
          filters <- Config.filters
          files   <- FileSystem.listFiles(path)
          cache   <- FileSystem.findCache(path)
          _       <- ZIO.foreach(files)(handleFile(channel, filters))
        } yield ()

      private def handleFile(
          channel: ScannerChannel,
          filters: List[Filter]
      )(file: File) =
        for {
          isDir      <- FileSystem.isDirectory(file)
          isIncluded <- UIO(Filters.isIncluded(file.toPath)(filters))
          _          <- ZIO.when(isIncluded && isDir)(scanPath(channel)(file.toPath))
          _          <- ZIO.when(isIncluded && !isDir)(sendHashedFile(channel)(file))
        } yield ()

      private def sendHashedFile(channel: ScannerChannel)(file: File) =
        for {
          sources   <- Config.sources
          source    <- Sources.forPath(file.toPath)(sources)
          prefix    <- Config.prefix
          hashes    <- Hasher.hashObject(file.toPath)
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
