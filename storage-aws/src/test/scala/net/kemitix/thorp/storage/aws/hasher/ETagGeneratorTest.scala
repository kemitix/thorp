package net.kemitix.thorp.storage.aws.hasher

import com.amazonaws.services.s3.transfer.TransferManagerConfiguration
import net.kemitix.thorp.domain.HashType.MD5
import net.kemitix.thorp.domain.MD5Hash
import net.kemitix.thorp.filesystem.{FileSystem, Hasher, Resource}
import zio.DefaultRuntime
import org.scalatest.freespec.AnyFreeSpec

class ETagGeneratorTest extends AnyFreeSpec {

  private val bigFile       = Resource(this, "../big-file")
  private val bigFilePath   = bigFile.toPath
  private val configuration = new TransferManagerConfiguration
  private val chunkSize     = 1200000
  configuration.setMinimumUploadPartSize(chunkSize)

  "Create offsets" - {
    "should create offsets" in {
      val offsets = ETagGenerator
        .offsets(bigFile.length, chunkSize)
        .foldRight(List[Long]())((l: Long, a: List[Long]) => l :: a)
      assertResult(
        List(0, chunkSize, chunkSize * 2, chunkSize * 3, chunkSize * 4))(
        offsets)
    }
  }

  private val runtime: DefaultRuntime = new DefaultRuntime {}
  object TestEnv extends Hasher.Live with FileSystem.Live

  "create md5 hash for each chunk" - {
    "should create expected hash for chunks" in {
      val md5Hashes = List(
        "68b7d37e6578297621e06f01800204f1",
        "973475b14a7bda6ad8864a7f9913a947",
        "b9adcfc5b103fe2dd5924a5e5e6817f0",
        "5bd6e10a99fef100fe7bf5eaa0a42384",
        "8a0c1d0778ac8fcf4ca2010eba4711eb"
      ).zipWithIndex
      md5Hashes.foreach {
        case (hash, index) =>
          val program = Hasher.hashObjectChunk(bigFilePath, index, chunkSize)
          val result  = runtime.unsafeRunSync(program.provide(TestEnv)).toEither
          assertResult(Right(hash))(
            result
              .map(_(MD5))
              .map(MD5Hash.hash))
      }
    }
  }

  "create etag for whole file" - {
    val expected = "f14327c90ad105244c446c498bfe9a7d-2"
    "should match aws etag for the file" in {
      val program = ETagGenerator.eTag(bigFilePath)
      val result  = runtime.unsafeRunSync(program.provide(TestEnv)).toEither
      assertResult(Right(expected))(result)
    }
  }

}
