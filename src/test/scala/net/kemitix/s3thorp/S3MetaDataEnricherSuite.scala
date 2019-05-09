package net.kemitix.s3thorp

import java.io.File
import java.nio.file.Paths
import java.time.Instant

import cats.effect.IO
import org.scalatest.FunSpec

class S3MetaDataEnricherSuite extends FunSpec {

  private val sourcePath = "/root/from/here/"
  private val source = Paths.get(sourcePath).toFile
  private val prefix = "prefix"
  private val config = Config("bucket", prefix, source)

  new S3MetaDataEnricher with DummyS3Client {
    describe("key generator") {
      val subject = generateKey(config)_

      def resolve(subdir: String): File = {
        source.toPath.resolve(subdir).toFile
      }

      describe("when file is within source") {
        it("has a valid key") {
          val subdir = "subdir"
          assertResult(s"$prefix/$subdir")(subject(resolve(subdir)))
        }
      }

      describe("when file is deeper within source") {
        it("has a valid key") {
          val subdir = "subdir/deeper/still"
          assertResult(s"$prefix/$subdir")(subject(resolve(subdir)))
        }
      }
    }
  }

  describe("enrich with metadata") {
    val local = "localFile"
    val localFile = new File(sourcePath + local)
    describe("when remote exists") {
      val hash = "hash"
      val lastModified = Instant.now()
      new S3MetaDataEnricher with DummyS3Client {
        override def objectHead(bucket: String, key: String) = IO(Some((hash, lastModified)))
        it("returns metadata") {
          val expectedMetadata = S3MetaData(localFile, s"$prefix/$local", hash, lastModified)

          val result = enrichWithS3MetaData(config)(localFile).compile.toList.unsafeRunSync().head
          assertResult(Right(expectedMetadata))(result)
        }
      }
    }
    describe("when remote doesn't exist") {
      new S3MetaDataEnricher with DummyS3Client {
        override def objectHead(bucket: String, key: String) = IO(None)
        it("returns file to upload") {
          val result = enrichWithS3MetaData(config)(localFile).compile.toList.unsafeRunSync().head
          assertResult(Left(localFile))(result)
        }
      }
    }
  }
}
