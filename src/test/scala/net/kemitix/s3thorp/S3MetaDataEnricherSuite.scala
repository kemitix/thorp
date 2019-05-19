package net.kemitix.s3thorp

import java.io.File
import java.time.Instant

import net.kemitix.s3thorp.awssdk.S3ObjectsData

class S3MetaDataEnricherSuite
  extends UnitTest
    with KeyGenerator {

  private val source = Resource(this, "upload")
  private val prefix = RemoteKey("prefix")
  implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)
  private val fileToKey = generateKey(config.source, config.prefix) _
  val lastModified = LastModified(Instant.now())

  describe("enrich with metadata") {
    new S3MetaDataEnricher with DummyS3Client {
      describe("#1 remote key exists, hash dpes not match, hash of other keys match") {pending}
      describe("#2 remote key exists, hash does not match, hash of other keys do not match") {
        val newLocalHash: MD5Hash = MD5Hash("the-new-hash")
        val theFile: LocalFile = aLocalFile("the-file", newLocalHash, source, fileToKey)
        val remoteKey: RemoteKey = aRemoteKey(prefix, "the-file")
        val originalHash: MD5Hash = MD5Hash("the-original-hash")
        val remoteMetadata = RemoteMetaData(remoteKey, originalHash, lastModified)
        implicit val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(originalHash -> Set(KeyModified(remoteKey, lastModified))),
          byKey = Map(remoteKey -> HashModified(originalHash, lastModified))
        )
        it("generates valid metadata") {
          val expected = S3MetaData(theFile, matchByHash = Set.empty, matchByKey = Some(remoteMetadata))
          val result = getMetadata(theFile)
          assertResult(expected)(result)
        }
      }
      describe("#3 remote key exists, hash matches, hash of other keys match") {
        val hash: MD5Hash = MD5Hash("the-file-hash")
        val theFile: LocalFile = aLocalFile("the-file", hash, source, fileToKey)
        val remoteKey: RemoteKey = aRemoteKey(prefix, "the-file")
        val remoteMetadata = RemoteMetaData(remoteKey, hash, lastModified)
        implicit val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(hash -> Set(KeyModified(remoteKey, lastModified))),
          byKey = Map(remoteKey -> HashModified(hash, lastModified))
        )
        it("generates valid metadata") {
          val expected = S3MetaData(theFile, matchByHash = Set(remoteMetadata), matchByKey = Some(remoteMetadata))
          val result = getMetadata(theFile)
          assertResult(expected)(result)
        }
      }
      describe("#4 remote key exists, hash matches, hash of other keys do not match") {
        pending
        val hash: MD5Hash = MD5Hash("the-file-hash")
        val theFile: LocalFile = aLocalFile("the-file", hash, source, fileToKey)
        val remoteKey: RemoteKey = aRemoteKey(prefix, "the-file")
        val remoteMetadata = RemoteMetaData(remoteKey, hash, lastModified)
        implicit val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(hash -> Set(KeyModified(remoteKey, lastModified))),
          byKey = Map(remoteKey -> HashModified(hash, lastModified))
        )
        it("generates valid metadata") {
          val expected = S3MetaData(theFile, matchByHash = Set(remoteMetadata), matchByKey = Some(remoteMetadata))
          val result = getMetadata(theFile)
          assertResult(expected)(result)
        }
      }
      describe("#5 remote key is missing, hash of other keys match") {pending}
      describe("#6 remote key is missing, hash of other keys do not match") {pending}
    }
  }
}
