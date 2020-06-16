package net.kemitix.thorp.lib

import java.util.concurrent.atomic.AtomicReference

import scala.jdk.CollectionConverters._

import net.kemitix.eip.zio.MessageChannel
import net.kemitix.thorp.config.{
  Config,
  ConfigOption,
  ConfigOptions,
  ConfigurationBuilder
}
import net.kemitix.thorp.domain.RemoteKey
import net.kemitix.thorp.filesystem.Resource
import net.kemitix.thorp.lib.FileScanner.ScannedFile
import org.scalatest.FreeSpec
import zio.clock.Clock
import zio.{DefaultRuntime, Ref, UIO, ZIO}

class FileScannerTest extends FreeSpec {

  "scanSources" - {
    "creates a FileSender for files in resources" in {
      def receiver(scanned: Ref[List[RemoteKey]])
        : UIO[MessageChannel.UReceiver[Any, ScannedFile]] = UIO { message =>
        for {
          _ <- scanned.update(l => message.body.remoteKey :: l)
        } yield ()
      }
      val scannedFiles =
        new AtomicReference[List[RemoteKey]](List.empty)
      val sourcePath = Resource.select(this, "upload").toPath
      val configOptions: List[ConfigOption] =
        List[ConfigOption](ConfigOption.Source(sourcePath),
                           ConfigOption.Bucket("bucket"),
                           ConfigOption.IgnoreGlobalOptions,
                           ConfigOption.IgnoreUserOptions)
      val program: ZIO[Clock with Config with FileScanner, Throwable, Unit] =
        for {
          config <- ConfigurationBuilder.buildConfig(
            ConfigOptions(configOptions.asJava))
          _          <- Config.set(config)
          scanner    <- FileScanner.scanSources
          scannedRef <- Ref.make[List[RemoteKey]](List.empty)
          receiver   <- receiver(scannedRef)
          _          <- MessageChannel.pointToPoint(scanner)(receiver).runDrain
          scanned    <- scannedRef.get
          _          <- UIO(scannedFiles.set(scanned))
        } yield ()
      object TestEnv extends FileScanner.Live with Clock.Live with Config.Live
      val completed =
        new DefaultRuntime {}.unsafeRunSync(program.provide(TestEnv)).toEither
      assert(completed.isRight)
      assertResult(
        Set(RemoteKey.create("root-file"),
            RemoteKey.create("subdir/leaf-file")))(scannedFiles.get.toSet)
    }

  }

}
