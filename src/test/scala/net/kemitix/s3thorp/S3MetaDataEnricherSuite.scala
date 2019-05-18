package net.kemitix.s3thorp

import java.io.File
import java.time.Instant

import net.kemitix.s3thorp.awssdk.S3ObjectsData
import org.scalatest.FunSpec

class S3MetaDataEnricherSuite
  extends FunSpec
    with KeyGenerator {

  private val source = Resource(this, "upload")
  private val prefix = RemoteKey("prefix")
  implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)
  private val fileToKey = generateKey(config.source, config.prefix) _

  new S3MetaDataEnricher with DummyS3Client {
    describe("key generator") {
      def resolve(subdir: String): File = {
        source.toPath.resolve(subdir).toFile
      }

      describe("when file is within source") {
        it("has a valid key") {
          val subdir = "subdir"
          assertResult(RemoteKey(s"${prefix.key}/$subdir"))(fileToKey(resolve(subdir)))
        }
      }

      describe("when file is deeper within source") {
        it("has a valid key") {
          val subdir = "subdir/deeper/still"
          assertResult(RemoteKey(s"${prefix.key}/$subdir"))(fileToKey(resolve(subdir)))
        }
      }
    }
  }

  describe("enrich with metadata") {
    val local = "localFile"
    val fileWithRemote = LocalFile(source.toPath.resolve(local).toFile, source, fileToKey)
    val fileWithNoRemote = LocalFile(source.toPath.resolve("noRemote").toFile, source, fileToKey)
    val remoteKey = RemoteKey(prefix.key + "/" + local)
    val hash = MD5Hash("hash")
    val lastModified = LastModified(Instant.now())
    implicit val s3ObjectsData: S3ObjectsData = S3ObjectsData(
      byHash = Map(hash -> (remoteKey, lastModified)),
      byKey = Map(remoteKey -> HashModified(hash, lastModified))
    )
    describe("when remote exists") {
      new S3MetaDataEnricher with DummyS3Client {
        it("returns metadata") {
          val expectedMetadata = S3MetaData(fileWithRemote, Some(RemoteMetaData(remoteKey, hash, lastModified)))

          val result = getMetadata(fileWithRemote)
          assertResult(expectedMetadata)(result)
        }
      }
    }
    describe("when remote doesn't exist") {
      new S3MetaDataEnricher with DummyS3Client {
        it("returns file to upload") {
          val result = getMetadata(fileWithNoRemote)
          assertResult(S3MetaData(fileWithNoRemote, None))(result)
        }
      }
    }
  }
}
