package net.kemitix.s3thorp

import java.io.File
import java.time.Instant

import net.kemitix.s3thorp.domain.{Bucket, Config, HashModified, KeyModified, LastModified, LocalFile, MD5Hash, RemoteKey, RemoteMetaData, S3MetaData, S3ObjectsData}
import org.scalatest.FunSpec

class S3MetaDataEnricherSuite
  extends FunSpec
    with KeyGenerator {

  private val source = Resource(this, "upload")
  private val prefix = RemoteKey("prefix")
  implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)
  private val fileToKey = generateKey(config.source, config.prefix) _
  private val fileToHash = (file: File) => new MD5HashGenerator {}.md5File(file)
  val lastModified = LastModified(Instant.now())

  describe("enrich with metadata") {
    new S3MetaDataEnricher with DummyS3Client {

      describe("#1a local exists, remote exists, remote matches, other matches - do nothing") {
        val theHash: MD5Hash = MD5Hash("the-file-hash")
        val theFile: LocalFile = LocalFile.resolve("the-file", theHash, source, fileToKey, fileToHash)
        val theRemoteKey: RemoteKey = theFile.remoteKey
        implicit val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(theHash -> Set(KeyModified(theRemoteKey, lastModified))),
          byKey = Map(theRemoteKey -> HashModified(theHash, lastModified))
        )
        val theRemoteMetadata = RemoteMetaData(theRemoteKey, theHash, lastModified)
        it("generates valid metadata") {
          val expected = Stream(S3MetaData(theFile,
            matchByHash = Set(theRemoteMetadata),
            matchByKey = Some(theRemoteMetadata)))
          val result = getMetadata(theFile)
          assertResult(expected)(result)
        }
      }
      describe("#1b local exists, remote exists, remote matches, other no matches - do nothing") {
        val theHash: MD5Hash = MD5Hash("the-file-hash")
        val theFile: LocalFile = LocalFile.resolve("the-file", theHash, source, fileToKey, fileToHash)
        val theRemoteKey: RemoteKey = prefix.resolve("the-file")
        implicit val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(theHash -> Set(KeyModified(theRemoteKey, lastModified))),
          byKey = Map(theRemoteKey -> HashModified(theHash, lastModified))
        )
        val theRemoteMetadata = RemoteMetaData(theRemoteKey, theHash, lastModified)
        it("generates valid metadata") {
          val expected = Stream(S3MetaData(theFile,
            matchByHash = Set(theRemoteMetadata),
            matchByKey = Some(theRemoteMetadata)))
          val result = getMetadata(theFile)
          assertResult(expected)(result)
        }
      }
      describe("#2 local exists, remote is missing, remote no match, other matches - copy") {
        val theHash = MD5Hash("the-hash")
        val theFile = LocalFile.resolve("the-file", theHash, source, fileToKey, fileToHash)
        val otherRemoteKey = RemoteKey("other-key")
        implicit val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(theHash -> Set(KeyModified(otherRemoteKey, lastModified))),
          byKey = Map(otherRemoteKey -> HashModified(theHash, lastModified))
        )
        val otherRemoteMetadata = RemoteMetaData(otherRemoteKey, theHash, lastModified)
        it("generates valid metadata") {
          val expected = Stream(S3MetaData(theFile,
            matchByHash = Set(otherRemoteMetadata),
            matchByKey = None))
          val result = getMetadata(theFile)
          assertResult(expected)(result)
        }
      }
      describe("#3 local exists, remote is missing, remote no match, other no matches - upload") {
        val theHash = MD5Hash("the-hash")
        val theFile = LocalFile.resolve("the-file", theHash, source, fileToKey, fileToHash)
        implicit val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(),
          byKey = Map()
        )
        it("generates valid metadata") {
          val expected = Stream(S3MetaData(theFile,
            matchByHash = Set.empty,
            matchByKey = None))
          val result = getMetadata(theFile)
          assertResult(expected)(result)
        }
      }
      describe("#4 local exists, remote exists, remote no match, other matches - copy") {
        val theHash = MD5Hash("the-hash")
        val theFile = LocalFile.resolve("the-file", theHash, source, fileToKey, fileToHash)
        val theRemoteKey = theFile.remoteKey
        val oldHash = MD5Hash("old-hash")
        val otherRemoteKey = prefix.resolve("other-key")
        implicit val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(
            oldHash -> Set(KeyModified(theRemoteKey, lastModified)),
            theHash -> Set(KeyModified(otherRemoteKey, lastModified))),
          byKey = Map(
            theRemoteKey -> HashModified(oldHash, lastModified),
            otherRemoteKey -> HashModified(theHash, lastModified)
          )
        )
        val theRemoteMetadata = RemoteMetaData(theRemoteKey, oldHash, lastModified)
        val otherRemoteMetadata = RemoteMetaData(otherRemoteKey, theHash, lastModified)
        it("generates valid metadata") {
          val expected = Stream(S3MetaData(theFile,
            matchByHash = Set(otherRemoteMetadata),
            matchByKey = Some(theRemoteMetadata)))
          val result = getMetadata(theFile)
          assertResult(expected)(result)
        }
      }
      describe("#5 local exists, remote exists, remote no match, other no matches - upload") {
        val theHash = MD5Hash("the-hash")
        val theFile = LocalFile.resolve("the-file", theHash, source, fileToKey, fileToHash)
        val theRemoteKey = theFile.remoteKey
        val oldHash = MD5Hash("old-hash")
        implicit val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(
            oldHash -> Set(KeyModified(theRemoteKey, lastModified)),
            theHash -> Set.empty),
          byKey = Map(
            theRemoteKey -> HashModified(oldHash, lastModified)
          )
        )
        val theRemoteMetadata = RemoteMetaData(theRemoteKey, oldHash, lastModified)
        it("generates valid metadata") {
          val expected = Stream(S3MetaData(theFile,
            matchByHash = Set.empty,
            matchByKey = Some(theRemoteMetadata)))
          val result = getMetadata(theFile)
          assertResult(expected)(result)
        }
      }
    }
  }
}
