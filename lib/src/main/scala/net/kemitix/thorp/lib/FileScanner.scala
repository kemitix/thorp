package net.kemitix.thorp.lib

import java.io.File
import java.nio.file.Path

import net.kemitix.eip.zio.MessageChannel.{EChannel, ESender}
import net.kemitix.eip.zio.{Message, MessageChannel}
import net.kemitix.thorp.config.Config
import net.kemitix.thorp.domain.{
  HashType,
  LocalFile,
  MD5Hash,
  RemoteKey,
  Sources
}
import net.kemitix.thorp.filesystem.{FileSystem, Hasher}
import zio.clock.Clock
import zio.{RIO, ZIO}

trait FileScanner {
  val fileScanner: FileScanner.Service
}

object FileScanner {

  type RemoteHashes = Map[MD5Hash, RemoteKey]
  type Hashes       = Map[HashType, MD5Hash]
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
          files <- FileSystem.listFiles(path)
          _     <- ZIO.foreach(files)(handleFile(channel))
        } yield ()

      private def handleFile(channel: ScannerChannel)(file: File) =
        for {
          isDir <- FileSystem.isDirectory(file)
          _     <- ZIO.when(isDir)(scanPath(channel)(file.toPath))
          _     <- ZIO.when(!isDir)(sendHashedFile(channel)(file))
        } yield ()

      private def sendHashedFile(channel: ScannerChannel)(file: File) =
        for {
          sources    <- Config.sources
          source     <- Sources.forPath(file.toPath)(sources)
          hashes     <- Hasher.hashObject(file.toPath)
          remoteKey  <- ZIO(RemoteKey.fromSourcePath(source, file.toPath))
          localFile  <- ZIO(LocalFile(file, source.toFile, hashes, remoteKey))
          hashedFile <- Message.create(localFile)
          _          <- MessageChannel.send(channel)(hashedFile)
        } yield ()
    }

  }
  object Live extends Live
  final def scanSources: RIO[FileScanner, FileSender] =
    ZIO.accessM(_.fileScanner.scanSources)
}
