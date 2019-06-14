package net.kemitix.s3thorp.core

import java.time.Instant

import net.kemitix.s3thorp.core.S3MetaDataEnricher.{getMetadata, getS3Status}
import net.kemitix.s3thorp.domain._
import org.scalatest.FunSpec

class S3MetaDataEnricherSuite
  extends FunSpec {

  private val source = Resource(this, "upload")
  private val prefix = RemoteKey("prefix")
  implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)
  private val fileToKey = KeyGenerator.generateKey(config.source, config.prefix) _
  val lastModified = LastModified(Instant.now())

  describe("enrich with metadata") {

      describe("#1a local exists, remote exists, remote matches, other matches - do nothing") {
        val theHash: MD5Hash = MD5Hash("the-file-hash")
        val theFile: LocalFile = LocalFile.resolve("the-file", theHash, source, fileToKey)
        val theRemoteKey: RemoteKey = theFile.remoteKey
        val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(theHash -> Set(KeyModified(theRemoteKey, lastModified))),
          byKey = Map(theRemoteKey -> HashModified(theHash, lastModified))
        )
        val theRemoteMetadata = RemoteMetaData(theRemoteKey, theHash, lastModified)
        it("generates valid metadata") {
          val expected = S3MetaData(theFile,
            matchByHash = Set(theRemoteMetadata),
            matchByKey = Some(theRemoteMetadata))
          val result = getMetadata(theFile, s3)
          assertResult(expected)(result)
        }
      }
      describe("#1b local exists, remote exists, remote matches, other no matches - do nothing") {
        val theHash: MD5Hash = MD5Hash("the-file-hash")
        val theFile: LocalFile = LocalFile.resolve("the-file", theHash, source, fileToKey)
        val theRemoteKey: RemoteKey = prefix.resolve("the-file")
        val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(theHash -> Set(KeyModified(theRemoteKey, lastModified))),
          byKey = Map(theRemoteKey -> HashModified(theHash, lastModified))
        )
        val theRemoteMetadata = RemoteMetaData(theRemoteKey, theHash, lastModified)
        it("generates valid metadata") {
          val expected = S3MetaData(theFile,
            matchByHash = Set(theRemoteMetadata),
            matchByKey = Some(theRemoteMetadata))
          val result = getMetadata(theFile, s3)
          assertResult(expected)(result)
        }
      }
      describe("#2 local exists, remote is missing, remote no match, other matches - copy") {
        val theHash = MD5Hash("the-hash")
        val theFile = LocalFile.resolve("the-file", theHash, source, fileToKey)
        val otherRemoteKey = RemoteKey("other-key")
        val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(theHash -> Set(KeyModified(otherRemoteKey, lastModified))),
          byKey = Map(otherRemoteKey -> HashModified(theHash, lastModified))
        )
        val otherRemoteMetadata = RemoteMetaData(otherRemoteKey, theHash, lastModified)
        it("generates valid metadata") {
          val expected = S3MetaData(theFile,
            matchByHash = Set(otherRemoteMetadata),
            matchByKey = None)
          val result = getMetadata(theFile, s3)
          assertResult(expected)(result)
        }
      }
      describe("#3 local exists, remote is missing, remote no match, other no matches - upload") {
        val theHash = MD5Hash("the-hash")
        val theFile = LocalFile.resolve("the-file", theHash, source, fileToKey)
        val s3: S3ObjectsData = S3ObjectsData(
          byHash = Map(),
          byKey = Map()
        )
        it("generates valid metadata") {
          val expected = S3MetaData(theFile,
            matchByHash = Set.empty,
            matchByKey = None)
          val result = getMetadata(theFile, s3)
          assertResult(expected)(result)
        }
      }
      describe("#4 local exists, remote exists, remote no match, other matches - copy") {
        val theHash = MD5Hash("the-hash")
        val theFile = LocalFile.resolve("the-file", theHash, source, fileToKey)
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
          val expected = S3MetaData(theFile,
            matchByHash = Set(otherRemoteMetadata),
            matchByKey = Some(theRemoteMetadata))
          val result = getMetadata(theFile, s3)
          assertResult(expected)(result)
        }
      }
      describe("#5 local exists, remote exists, remote no match, other no matches - upload") {
        val theHash = MD5Hash("the-hash")
        val theFile = LocalFile.resolve("the-file", theHash, source, fileToKey)
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
          val expected = S3MetaData(theFile,
            matchByHash = Set.empty,
            matchByKey = Some(theRemoteMetadata))
          val result = getMetadata(theFile, s3)
          assertResult(expected)(result)
        }
      }
  }

  describe("getS3Status") {
    val hash = MD5Hash("hash")
    val localFile = LocalFile.resolve("the-file", hash, source, fileToKey)
    val key = localFile.remoteKey
    val keyOtherKey = LocalFile.resolve("other-key-same-hash", hash, source, fileToKey)
    val diffHash = MD5Hash("diff")
    val keyDiffHash = LocalFile.resolve("other-key-diff-hash", diffHash, source, fileToKey)
    val lastModified = LastModified(Instant.now)
    val s3ObjectsData: S3ObjectsData = S3ObjectsData(
      byHash = Map(
        hash -> Set(KeyModified(key, lastModified), KeyModified(keyOtherKey.remoteKey, lastModified)),
        diffHash -> Set(KeyModified(keyDiffHash.remoteKey, lastModified))),
      byKey = Map(
        key -> HashModified(hash, lastModified),
        keyOtherKey.remoteKey -> HashModified(hash, lastModified),
        keyDiffHash.remoteKey -> HashModified(diffHash, lastModified)))

    def invoke(localFile: LocalFile) = {
      getS3Status(localFile, s3ObjectsData)
    }

    describe("when remote key exists") {
      it("should return (Some, Set.nonEmpty)") {
        assertResult(
          (Some(HashModified(hash, lastModified)),
            Set(
              KeyModified(key, lastModified),
              KeyModified(keyOtherKey.remoteKey, lastModified)))
        )(invoke(localFile))
      }
    }

    describe("when remote key does not exist and no others matches hash") {
      it("should return (None, Set.empty)") {
        val localFile = LocalFile.resolve("missing-file", MD5Hash("unique"), source, fileToKey)
        assertResult(
          (None,
            Set.empty)
        )(invoke(localFile))
      }
    }

    describe("when remote key exists and no others match hash") {
      it("should return (None, Set.nonEmpty)") {
        assertResult(
          (Some(HashModified(diffHash, lastModified)),
            Set(KeyModified(keyDiffHash.remoteKey, lastModified)))
        )(invoke(keyDiffHash))
      }
    }

  }

}
