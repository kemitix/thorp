package net.kemitix.s3thorp

import java.io.File
import java.nio.file.Paths
import java.time.Instant

import net.kemitix.s3thorp.awssdk.HashLookup
import org.scalatest.FunSpec

class S3MetaDataEnricherSuite extends FunSpec {

  private val sourcePath = "/root/from/here/"
  private val source = Paths.get(sourcePath).toFile
  private val prefix = "prefix"
  private val config = Config(Bucket("bucket"), prefix, source = source)

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
    val fileWithRemote = new File(sourcePath + local)
    val fileWithNoRemote = new File(sourcePath + "noRemote")
    val remoteKey = prefix + "/" + local
    val hash = "hash"
    val lastModified = Instant.now()
    val hashLookup = HashLookup(
      byHash = Map(hash -> (remoteKey, lastModified)),
      byKey = Map(remoteKey -> (hash, lastModified))
    )
    describe("when remote exists") {
      new S3MetaDataEnricher with DummyS3Client {
        it("returns metadata") {
          val expectedMetadata = S3MetaData(fileWithRemote, remoteKey, hash, lastModified)

          val result = enrichWithS3MetaData(config)(hashLookup)(fileWithRemote)
          assertResult(Right(expectedMetadata))(result)
        }
      }
    }
    describe("when remote doesn't exist") {
      new S3MetaDataEnricher with DummyS3Client {
        it("returns file to upload") {
          val result = enrichWithS3MetaData(config)(hashLookup)(fileWithNoRemote)
          assertResult(Left(fileWithNoRemote))(result)
        }
      }
    }
  }
}
