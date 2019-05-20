package net.kemitix.s3thorp

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

      describe("#1a local exists, remote exists, remote matches, other matches - do nothing") {
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
      describe("#1b local exists, remote exists, remote matches, other no matches - do nothing") {
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
      describe("#2 local exists, remote is missing, remote no match, other matches - copy") {
        val hash = MD5Hash("the-hash")
        val theFile = aLocalFile("the-file", hash, source, fileToKey)
        val otherRemoteKey = RemoteKey("other-key")
        val otherKeyMetadata = RemoteMetaData(otherRemoteKey, hash, lastModified)
        implicit val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(hash -> Set(KeyModified(otherRemoteKey, lastModified))),
          byKey = Map(otherRemoteKey -> HashModified(hash, lastModified))
        )
        it("I should write this test"){
          val expected = S3MetaData(theFile,
            matchByHash = Set(otherKeyMetadata),
            matchByKey = None)
          val result = getMetadata(theFile)
          assertResult(expected)(result)
        }
      }
      describe("#3 local exists, remote is missing, remote no match, other no matches - upload") {
        val hash = MD5Hash("the-hash")
        val theFile = aLocalFile("the-file", hash, source, fileToKey)
        implicit val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(),
          byKey = Map()
        )
        it("I should write this test"){
          val expected = S3MetaData(theFile,
            matchByHash = Set.empty,
            matchByKey = None)
          val result = getMetadata(theFile)
          assertResult(expected)(result)
        }
      }

      describe("#4 local exists, remote exists, remote no match, other matches - copy") {it("I should write this test"){pending}}
      describe("#5 local exists, remote exists, remote no match, other no matches - upload") {
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
      describe("#6 local missing, remote exists - delete") {it("I should write this test"){pending}}
    }
  }
}
