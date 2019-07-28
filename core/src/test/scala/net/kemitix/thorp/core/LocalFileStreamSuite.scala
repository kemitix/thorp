package net.kemitix.thorp.core

import java.nio.file.Paths

import net.kemitix.thorp.config._
import net.kemitix.thorp.console._
import net.kemitix.thorp.domain.HashType.MD5
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.{HashService, Storage}
import org.scalatest.FunSpec
import zio.{DefaultRuntime, Task, UIO}

class LocalFileStreamSuite extends FunSpec {

  private val source     = Resource(this, "upload")
  private val sourcePath = source.toPath
  private val hashService: HashService = DummyHashService(
    Map(
      file("root-file")        -> Map(MD5 -> MD5HashData.Root.hash),
      file("subdir/leaf-file") -> Map(MD5 -> MD5HashData.Leaf.hash)
    ))

  private def file(filename: String) =
    sourcePath.resolve(Paths.get(filename))

  private val configOptions = ConfigOptions(
    List(
      ConfigOption.IgnoreGlobalOptions,
      ConfigOption.IgnoreUserOptions,
      ConfigOption.Source(sourcePath),
      ConfigOption.Bucket("aBucket")
    ))

  describe("findFiles") {
    it("should find all files") {
      val expected = Right(Set("subdir/leaf-file", "root-file"))
      val result =
        invoke()
          .map(_.localFiles)
          .map(localFiles => localFiles.map(_.relative.toString))
          .map(_.toSet)
      assertResult(expected)(result)
    }
    it("should count all files") {
      val expected = Right(2)
      val result   = invoke().map(_.count)
      assertResult(expected)(result)
    }
    it("should sum the size of all files") {
      val expected = Right(113)
      val result   = invoke().map(_.totalSizeBytes)
      assertResult(expected)(result)
    }
  }

  private def invoke() = {
    type TestEnv = Storage with Console with Config
    val testEnv: TestEnv = new Storage.Test with Console.Test with Config.Live {
      override def listResult: Task[S3ObjectsData] =
        Task.die(new NotImplementedError)
      override def uploadResult: UIO[StorageQueueEvent] =
        Task.die(new NotImplementedError)
      override def copyResult: UIO[StorageQueueEvent] =
        Task.die(new NotImplementedError)
      override def deleteResult: UIO[StorageQueueEvent] =
        Task.die(new NotImplementedError)
      override def shutdownResult: UIO[StorageQueueEvent] =
        Task.die(new NotImplementedError)
    }

    def testProgram =
      for {
        config <- ConfigurationBuilder.buildConfig(configOptions)
        _      <- setConfig(config)
        files  <- LocalFileStream.findFiles(hashService)(sourcePath)
      } yield files

    new DefaultRuntime {}.unsafeRunSync {
      testProgram.provide(testEnv)
    }.toEither
  }

}
