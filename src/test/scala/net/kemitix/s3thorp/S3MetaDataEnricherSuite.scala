package net.kemitix.s3thorp

import java.io.File
import java.time.Instant

import net.kemitix.s3thorp.S3MetaDataEnricher.{getMetadata, getS3Status}
import net.kemitix.s3thorp.aws.api.S3Client
import net.kemitix.s3thorp.awssdk.S3ClientBuilder
import net.kemitix.s3thorp.domain.{Bucket, Config, HashModified, KeyModified, LastModified, LocalFile, MD5Hash, RemoteKey, RemoteMetaData, S3MetaData, S3ObjectsData}
import org.scalatest.FunSpec

class S3MetaDataEnricherSuite
  extends FunSpec {

  private val source = Resource(this, "upload")
  private val prefix = RemoteKey("prefix")
  implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)
  implicit private val logInfo: Int => String => Unit = l => i => ()
  private val fileToKey = KeyGenerator.generateKey(config.source, config.prefix) _
  private val fileToHash = (file: File) => MD5HashGenerator.md5File(file)
  val lastModified = LastModified(Instant.now())

  describe("enrich with metadata") {

      describe("#1a local exists, remote exists, remote matches, other matches - do nothing") {
        val theHash: MD5Hash = MD5Hash("the-file-hash")
        val theFile: LocalFile = LocalFile.resolve("the-file", theHash, source, fileToKey, fileToHash)
        val theRemoteKey: RemoteKey = theFile.remoteKey
        val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(theHash -> Set(KeyModified(theRemoteKey, lastModified))),
          byKey = Map(theRemoteKey -> HashModified(theHash, lastModified))
        )
        val theRemoteMetadata = RemoteMetaData(theRemoteKey, theHash, lastModified)
        it("generates valid metadata") {
          val expected = Stream(S3MetaData(theFile,
            matchByHash = Set(theRemoteMetadata),
            matchByKey = Some(theRemoteMetadata)))
          val result = getMetadata(theFile, s3)
          assertResult(expected)(result)
        }
      }
      describe("#1b local exists, remote exists, remote matches, other no matches - do nothing") {
        val theHash: MD5Hash = MD5Hash("the-file-hash")
        val theFile: LocalFile = LocalFile.resolve("the-file", theHash, source, fileToKey, fileToHash)
        val theRemoteKey: RemoteKey = prefix.resolve("the-file")
        val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(theHash -> Set(KeyModified(theRemoteKey, lastModified))),
          byKey = Map(theRemoteKey -> HashModified(theHash, lastModified))
        )
        val theRemoteMetadata = RemoteMetaData(theRemoteKey, theHash, lastModified)
        it("generates valid metadata") {
          val expected = Stream(S3MetaData(theFile,
            matchByHash = Set(theRemoteMetadata),
            matchByKey = Some(theRemoteMetadata)))
          val result = getMetadata(theFile, s3)
          assertResult(expected)(result)
        }
      }
      describe("#2 local exists, remote is missing, remote no match, other matches - copy") {
        val theHash = MD5Hash("the-hash")
        val theFile = LocalFile.resolve("the-file", theHash, source, fileToKey, fileToHash)
        val otherRemoteKey = RemoteKey("other-key")
        val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(theHash -> Set(KeyModified(otherRemoteKey, lastModified))),
          byKey = Map(otherRemoteKey -> HashModified(theHash, lastModified))
        )
        val otherRemoteMetadata = RemoteMetaData(otherRemoteKey, theHash, lastModified)
        it("generates valid metadata") {
          val expected = Stream(S3MetaData(theFile,
            matchByHash = Set(otherRemoteMetadata),
            matchByKey = None))
          val result = getMetadata(theFile, s3)
          assertResult(expected)(result)
        }
      }
      describe("#3 local exists, remote is missing, remote no match, other no matches - upload") {
        val theHash = MD5Hash("the-hash")
        val theFile = LocalFile.resolve("the-file", theHash, source, fileToKey, fileToHash)
        val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(),
          byKey = Map()
        )
        it("generates valid metadata") {
          val expected = Stream(S3MetaData(theFile,
            matchByHash = Set.empty,
            matchByKey = None))
          val result = getMetadata(theFile, s3)
          assertResult(expected)(result)
        }
      }
      describe("#4 local exists, remote exists, remote no match, other matches - copy") {
        val theHash = MD5Hash("the-hash")
        val theFile = LocalFile.resolve("the-file", theHash, source, fileToKey, fileToHash)
        val theRemoteKey = theFile.remoteKey
        val oldHash = MD5Hash("old-hash")
        val otherRemoteKey = prefix.resolve("other-key")
        val s3: S3ObjectsData = S3ObjectsData(
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
          val result = getMetadata(theFile, s3)
          assertResult(expected)(result)
        }
      }
      describe("#5 local exists, remote exists, remote no match, other no matches - upload") {
        val theHash = MD5Hash("the-hash")
        val theFile = LocalFile.resolve("the-file", theHash, source, fileToKey, fileToHash)
        val theRemoteKey = theFile.remoteKey
        val oldHash = MD5Hash("old-hash")
        val s3: S3ObjectsData = S3ObjectsData(
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
          val result = getMetadata(theFile, s3)
          assertResult(expected)(result)
        }
      }
  }

  describe("getS3Status") {
    val hash = MD5Hash("hash")
    val localFile = LocalFile.resolve("the-file", hash, source, fileToKey, fileToHash)
    val key = localFile.remoteKey
    val keyotherkey = LocalFile.resolve("other-key-same-hash", hash, source, fileToKey, fileToHash)
    val diffhash = MD5Hash("diff")
    val keydiffhash = LocalFile.resolve("other-key-diff-hash", diffhash, source, fileToKey, fileToHash)
    val lastModified = LastModified(Instant.now)
    val s3ObjectsData: S3ObjectsData = S3ObjectsData(
      byHash = Map(
        hash -> Set(KeyModified(key, lastModified), KeyModified(keyotherkey.remoteKey, lastModified)),
        diffhash -> Set(KeyModified(keydiffhash.remoteKey, lastModified))),
      byKey = Map(
        key -> HashModified(hash, lastModified),
        keyotherkey.remoteKey -> HashModified(hash, lastModified),
        keydiffhash.remoteKey -> HashModified(diffhash, lastModified)))

    def invoke(self: S3Client, localFile: LocalFile) = {
      getS3Status(localFile, s3ObjectsData)
    }

    describe("when remote key exists") {
      val s3Client = S3ClientBuilder.defaultClient
      it("should return (Some, Set.nonEmpty)") {
        assertResult(
          (Some(HashModified(hash, lastModified)),
            Set(
              KeyModified(key, lastModified),
              KeyModified(keyotherkey.remoteKey, lastModified)))
        )(invoke(s3Client, localFile))
      }
    }

    describe("when remote key does not exist and no others matches hash") {
      val s3Client = S3ClientBuilder.defaultClient
      it("should return (None, Set.empty)") {
        val localFile = LocalFile.resolve("missing-file", MD5Hash("unique"), source, fileToKey, fileToHash)
        assertResult(
          (None,
            Set.empty)
        )(invoke(s3Client, localFile))
      }
    }

    describe("when remote key exists and no others match hash") {
      val s3Client = S3ClientBuilder.defaultClient
      it("should return (None, Set.nonEmpty)") {
        assertResult(
          (Some(HashModified(diffhash, lastModified)),
            Set(KeyModified(keydiffhash.remoteKey, lastModified)))
        )(invoke(s3Client, keydiffhash))
      }
    }

  }

}
