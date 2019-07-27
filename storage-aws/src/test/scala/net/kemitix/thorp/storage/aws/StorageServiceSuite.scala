package net.kemitix.thorp.storage.aws

import java.time.Instant

import net.kemitix.thorp.core.{KeyGenerator, Resource, S3MetaDataEnricher}
import net.kemitix.thorp.domain.HashType.MD5
import net.kemitix.thorp.domain._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSpec

class StorageServiceSuite extends FunSpec with MockFactory {

  private val source     = Resource(this, "upload")
  private val sourcePath = source.toPath

  private val prefix = RemoteKey("prefix")
  implicit private val config: Config =
    Config(Bucket("bucket"), prefix, sources = Sources(List(sourcePath)))
  private val fileToKey =
    KeyGenerator.generateKey(config.sources, config.prefix) _

  describe("getS3Status") {

    val hash = MD5Hash("hash")
    val localFile =
      LocalFile.resolve("the-file", Map(MD5 -> hash), sourcePath, fileToKey)
    val key = localFile.remoteKey
    val keyOtherKey = LocalFile.resolve("other-key-same-hash",
                                        Map(MD5 -> hash),
                                        sourcePath,
                                        fileToKey)
    val diffHash = MD5Hash("diff")
    val keyDiffHash = LocalFile.resolve("other-key-diff-hash",
                                        Map(MD5 -> diffHash),
                                        sourcePath,
                                        fileToKey)
    val lastModified = LastModified(Instant.now)
    val s3ObjectsData: S3ObjectsData = S3ObjectsData(
      byHash = Map(
        hash -> Set(KeyModified(key, lastModified),
                    KeyModified(keyOtherKey.remoteKey, lastModified)),
        diffHash -> Set(KeyModified(keyDiffHash.remoteKey, lastModified))
      ),
      byKey = Map(
        key                   -> HashModified(hash, lastModified),
        keyOtherKey.remoteKey -> HashModified(hash, lastModified),
        keyDiffHash.remoteKey -> HashModified(diffHash, lastModified)
      )
    )

    def invoke(localFile: LocalFile) =
      S3MetaDataEnricher.getS3Status(localFile, s3ObjectsData)

    def getMatchesByKey(
        status: (Option[HashModified], Set[(MD5Hash, KeyModified)]))
      : Option[HashModified] = {
      val (byKey, _) = status
      byKey
    }

    def getMatchesByHash(
        status: (Option[HashModified], Set[(MD5Hash, KeyModified)]))
      : Set[(MD5Hash, KeyModified)] = {
      val (_, byHash) = status
      byHash
    }

    describe(
      "when remote key exists, unmodified and other key matches the hash") {
      it("should return the match by key") {
        val result = getMatchesByKey(invoke(localFile))
        assert(result.contains(HashModified(hash, lastModified)))
      }
      it("should return both matches for the hash") {
        val result = getMatchesByHash(invoke(localFile))
        assertResult(
          Set((hash, KeyModified(key, lastModified)),
              (hash, KeyModified(keyOtherKey.remoteKey, lastModified)))
        )(result)
      }
    }

    describe("when remote key does not exist and no others matches hash") {
      val localFile = LocalFile.resolve("missing-file",
                                        Map(MD5 -> MD5Hash("unique")),
                                        sourcePath,
                                        fileToKey)
      it("should return no matches by key") {
        val result = getMatchesByKey(invoke(localFile))
        assert(result.isEmpty)
      }
      it("should return no matches by hash") {
        val result = getMatchesByHash(invoke(localFile))
        assert(result.isEmpty)
      }
    }

    describe("when remote key exists and no others match hash") {
      val localFile = keyDiffHash
      it("should return the match by key") {
        val result = getMatchesByKey(invoke(localFile))
        assert(result.contains(HashModified(diffHash, lastModified)))
      }
      it("should return one match by hash") {
        val result = getMatchesByHash(invoke(localFile))
        assertResult(
          Set((diffHash, KeyModified(keyDiffHash.remoteKey, lastModified)))
        )(result)
      }
    }

  }

}
