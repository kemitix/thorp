package net.kemitix.thorp.storage.aws.hasher

import java.nio.file.Path

import com.amazonaws.services.s3.transfer.TransferManagerConfiguration
import net.kemitix.thorp.domain.HashType.MD5
import net.kemitix.thorp.domain.MD5Hash
import net.kemitix.thorp.filesystem.{FileSystem, Hasher, Resource}
import org.scalatest.FunSpec
import zio.DefaultRuntime

class ETagGeneratorTest extends FunSpec {

  object TestEnv extends Hasher.Live with FileSystem.Live

  private val bigFile       = Resource(this, "../big-file")
  private val bigFilePath   = bigFile.toPath
  private val configuration = new TransferManagerConfiguration
  private val chunkSize     = 1200000
  configuration.setMinimumUploadPartSize(chunkSize)

  describe("Create offsets") {
    it("should create offsets") {
      val offsets = ETagGenerator
        .offsets(bigFile.length, chunkSize)
        .foldRight(List[Long]())((l: Long, a: List[Long]) => l :: a)
      assertResult(
        List(0, chunkSize, chunkSize * 2, chunkSize * 3, chunkSize * 4))(
        offsets)
    }
  }

  describe("create md5 hash for each chunk") {
    it("should create expected hash for chunks") {
      val md5Hashes = List(
        "68b7d37e6578297621e06f01800204f1",
        "973475b14a7bda6ad8864a7f9913a947",
        "b9adcfc5b103fe2dd5924a5e5e6817f0",
        "5bd6e10a99fef100fe7bf5eaa0a42384",
        "8a0c1d0778ac8fcf4ca2010eba4711eb"
      ).zipWithIndex
      md5Hashes.foreach {
        case (hash, index) =>
          assertResult(Right(hash))(
            invoke(bigFilePath, index, chunkSize)
              .map(_(MD5))
              .map(MD5Hash.hash))
      }
    }
    def invoke(path: Path, index: Long, size: Long) =
      new DefaultRuntime {}.unsafeRunSync {
        Hasher
          .hashObjectChunk(path, index, size)
          .provide(TestEnv)
      }.toEither
  }

  describe("create etag for whole file") {
    val expected = "f14327c90ad105244c446c498bfe9a7d-2"
    it("should match aws etag for the file") {
      val result = invoke(bigFilePath)
      assertResult(Right(expected))(result)
    }
    def invoke(path: Path) = {
      new DefaultRuntime {}.unsafeRunSync {
        ETagGenerator
          .eTag(path)
          .provide(TestEnv)
      }.toEither
    }
  }

}
