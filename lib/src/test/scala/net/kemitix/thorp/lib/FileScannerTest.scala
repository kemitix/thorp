package net.kemitix.thorp.lib

import java.util.concurrent.atomic.AtomicReference

import net.kemitix.eip.zio.MessageChannel
import net.kemitix.thorp.config.{
  Config,
  ConfigOption,
  ConfigOptions,
  ConfigurationBuilder
}
import net.kemitix.thorp.domain.{LocalFile, RemoteKey}
import net.kemitix.thorp.filesystem.{FileSystem, Hasher, Resource}
import net.kemitix.thorp.lib.FileScanner.ScannedFile
import org.scalatest.FreeSpec
import zio.clock.Clock
import zio.{DefaultRuntime, Ref, UIO}

class FileScannerTest extends FreeSpec {

  "scanSources" - {
    "creates a FileSender for files in resources" in {
      def receiver(scanned: Ref[List[RemoteKey]])
        : UIO[MessageChannel.UReceiver[Any, ScannedFile]] = UIO { message =>
        for {
          _ <- scanned.update(l => LocalFile.remoteKey.get(message.body) :: l)
        } yield ()
      }
      val scannedFiles =
        new AtomicReference[List[RemoteKey]](List.empty)
      val sourcePath = Resource(this, "upload").toPath
      val configOptions: List[ConfigOption] =
        List[ConfigOption](ConfigOption.Source(sourcePath),
                           ConfigOption.Bucket("bucket"),
                           ConfigOption.IgnoreGlobalOptions,
                           ConfigOption.IgnoreUserOptions)
      val program = for {
        config     <- ConfigurationBuilder.buildConfig(ConfigOptions(configOptions))
        _          <- Config.set(config)
        scanner    <- FileScanner.scanSources
        scannedRef <- Ref.make[List[RemoteKey]](List.empty)
        receiver   <- receiver(scannedRef)
        _          <- MessageChannel.pointToPoint(scanner)(receiver).runDrain
        scanned    <- scannedRef.get
        _          <- UIO(scannedFiles.set(scanned))
      } yield ()
      object TestEnv
          extends FileScanner.Live
          with Clock.Live
          with Hasher.Live
          with FileSystem.Live
          with Config.Live
      val completed =
        new DefaultRuntime {}.unsafeRunSync(program.provide(TestEnv)).toEither
      assert(completed.isRight)
      assertResult(Set(RemoteKey("root-file"), RemoteKey("subdir/leaf-file")))(
        scannedFiles.get.toSet)
    }

  }

}
