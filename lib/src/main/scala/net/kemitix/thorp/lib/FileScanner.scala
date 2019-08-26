package net.kemitix.thorp.lib

import java.io.File
import java.nio.file.Path

import net.kemitix.eip.zio.MessageChannel.{EChannel, ESender}
import net.kemitix.eip.zio.{Message, MessageChannel}
import net.kemitix.thorp.config.Config
import net.kemitix.thorp.domain.{HashType, MD5Hash, RemoteKey}
import net.kemitix.thorp.filesystem.{FileSystem, Hasher}
import zio.clock.Clock
import zio.{RIO, ZIO}

trait FileScanner {
  val fileScanner: FileScanner.Service
}

object FileScanner {

  type RemoteHashes = Map[MD5Hash, RemoteKey]
  type Hashes       = Map[HashType, MD5Hash]
  type ScannedFile  = (File, Hashes)
  type Channel      = EChannel[Any, Throwable, ScannedFile]
  type Sender =
    ESender[Config with Clock with Hasher with FileSystem with FileScanner,
            Throwable,
            ScannedFile]
  type Env = Clock with FileSystem with Hasher

  trait Service {
    def scanSources: RIO[FileScanner, Sender]
  }

  trait Live extends FileScanner {
    val fileScanner: Service = new Service {

      override def scanSources: RIO[FileScanner, Sender] =
        RIO { channel =>
          for {
            sources <- Config.sources
            _       <- ZIO.foreach(sources.paths)(scanPath(channel)(_))
          } yield ()
        }

      private def scanPath(
          channel: Channel): Path => RIO[Env with FileScanner, Unit] =
        path =>
          for {
            files <- FileSystem.listFiles(path)
            _     <- ZIO.foreach(files)(handleFile(channel))
          } yield ()

      private def handleFile(
          channel: Channel): File => RIO[Env with FileScanner, Unit] =
        file =>
          for {
            isDir <- FileSystem.isDirectory(file)
            _     <- ZIO.when(isDir)(scanPath(channel)(file.toPath))
            _     <- ZIO.when(!isDir)(sendHashedFile(channel)(file))
          } yield ()

      private def sendHashedFile(channel: Channel): File => RIO[Env, Unit] =
        file =>
          for {
            hash       <- Hasher.hashObject(file.toPath)
            hashedFile <- Message.create((file, hash))
            _          <- MessageChannel.send(channel)(hashedFile)
          } yield ()
    }

  }
  object Live extends Live
  final def scanSources: RIO[FileScanner, Sender] =
    ZIO.accessM(_.fileScanner.scanSources)
}
