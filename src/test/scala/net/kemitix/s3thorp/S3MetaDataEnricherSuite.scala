package net.kemitix.s3thorp

import java.io.File
import java.nio.file.Paths
import java.time.Instant

import net.kemitix.s3thorp.awssdk.HashLookup
import org.scalatest.FunSpec

class S3MetaDataEnricherSuite extends FunSpec {

  private val sourcePath = "/root/from/here/"
  private val source = Paths.get(sourcePath).toFile
  private val prefix = RemoteKey("prefix")
  implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)

  new S3MetaDataEnricher with DummyS3Client {
    describe("key generator") {
      def resolve(subdir: String): File = {
        source.toPath.resolve(subdir).toFile
      }

      describe("when file is within source") {
        it("has a valid key") {
          val subdir = "subdir"
          assertResult(RemoteKey(s"${prefix.key}/$subdir"))(generateKey(resolve(subdir)))
        }
      }

      describe("when file is deeper within source") {
        it("has a valid key") {
          val subdir = "subdir/deeper/still"
          assertResult(RemoteKey(s"${prefix.key}/$subdir"))(generateKey(resolve(subdir)))
        }
      }
    }
  }

  describe("enrich with metadata") {
    val local = "localFile"
    val fileWithRemote = new File(sourcePath + local)
    val fileWithNoRemote = new File(sourcePath + "noRemote")
    val remoteKey = RemoteKey(prefix.key + "/" + local)
    val hash = MD5Hash("hash")
    val lastModified = LastModified(Instant.now())
    implicit val hashLookup: HashLookup = HashLookup(
      byHash = Map(hash -> (remoteKey, lastModified)),
      byKey = Map(remoteKey -> HashModified(hash, lastModified))
    )
    describe("when remote exists") {
      new S3MetaDataEnricher with DummyS3Client {
        it("returns metadata") {
          val expectedMetadata = S3MetaData(fileWithRemote, Some((remoteKey, hash, lastModified)))

          val result = enrichWithS3MetaData(fileWithRemote)
          assertResult(expectedMetadata)(result)
        }
      }
    }
    describe("when remote doesn't exist") {
      new S3MetaDataEnricher with DummyS3Client {
        it("returns file to upload") {
          val result = enrichWithS3MetaData(fileWithNoRemote)
          assertResult(S3MetaData(fileWithNoRemote, None))(result)
        }
      }
    }
  }
}
