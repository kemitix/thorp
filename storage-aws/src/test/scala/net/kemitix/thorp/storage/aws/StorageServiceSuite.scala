package net.kemitix.thorp.storage.aws

import java.time.Instant

import net.kemitix.thorp.config.Resource
import net.kemitix.thorp.core.{
  KeyGenerator,
  LocalFileValidator,
  S3MetaDataEnricher
}
import net.kemitix.thorp.domain.HashType.MD5
import net.kemitix.thorp.domain._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSpec

class StorageServiceSuite extends FunSpec with MockFactory {

  private val source     = Resource(this, "upload")
  private val sourcePath = source.toPath
  private val sources    = Sources(List(sourcePath))
  private val prefix     = RemoteKey("prefix")
  private val fileToKey =
    KeyGenerator.generateKey(sources, prefix) _

  describe("getS3Status") {

    val hash = MD5Hash("hash")
    val env = for {
      localFile <- LocalFileValidator.resolve("the-file",
                                              Map(MD5 -> hash),
                                              sourcePath,
                                              fileToKey)
      key = localFile.remoteKey
      keyOtherKey <- LocalFileValidator.resolve("other-key-same-hash",
                                                Map(MD5 -> hash),
                                                sourcePath,
                                                fileToKey)
      diffHash = MD5Hash("diff")
      keyDiffHash <- LocalFileValidator.resolve("other-key-diff-hash",
                                                Map(MD5 -> diffHash),
                                                sourcePath,
                                                fileToKey)
      lastModified = LastModified(Instant.now)
      s3ObjectsData = S3ObjectsData(
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
    } yield
      (s3ObjectsData,
       localFile: LocalFile,
       lastModified,
       keyOtherKey,
       keyDiffHash,
       diffHash,
       key)

    def invoke(localFile: LocalFile, s3ObjectsData: S3ObjectsData) =
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
        env.map({
          case (s3ObjectsData, localFile, lastModified, _, _, _, _) => {
            val result = getMatchesByKey(invoke(localFile, s3ObjectsData))
            assert(result.contains(HashModified(hash, lastModified)))
          }
        })
      }
      it("should return both matches for the hash") {
        env.map({
          case (s3ObjectsData,
                localFile,
                lastModified,
                keyOtherKey,
                _,
                _,
                key) => {
            val result = getMatchesByHash(invoke(localFile, s3ObjectsData))
            assertResult(
              Set((hash, KeyModified(key, lastModified)),
                  (hash, KeyModified(keyOtherKey.remoteKey, lastModified)))
            )(result)
          }
        })
      }
    }

    describe("when remote key does not exist and no others matches hash") {
      val env2 = LocalFileValidator
        .resolve("missing-file",
                 Map(MD5 -> MD5Hash("unique")),
                 sourcePath,
                 fileToKey)
      it("should return no matches by key") {
        env2.map(localFile => {
          env.map({
            case (s3ObjectsData, _, _, _, _, _, _) => {
              val result = getMatchesByKey(invoke(localFile, s3ObjectsData))
              assert(result.isEmpty)
            }
          })
        })
      }
      it("should return no matches by hash") {
        env2.map(localFile => {
          env.map({
            case (s3ObjectsData, _, _, _, _, _, _) => {
              val result = getMatchesByHash(invoke(localFile, s3ObjectsData))
              assert(result.isEmpty)
            }
          })
        })
      }
    }

    describe("when remote key exists and no others match hash") {
      it("should return the match by key") {
        env.map({
            case (s3ObjectsData,
                  _,
                  lastModified,
                  _,
                  keyDiffHash,
                  diffHash,
                  _) => {
              val result = getMatchesByKey(invoke(keyDiffHash, s3ObjectsData))
              assert(result.contains(HashModified(diffHash, lastModified)))
            }
          })
      }
      it("should return one match by hash") {
        env.map({
            case (s3ObjectsData,
                  _,
                  lastModified,
                  _,
                  keyDiffHash,
                  diffHash,
                  _) => {
              val result = getMatchesByHash(invoke(keyDiffHash, s3ObjectsData))
              assertResult(
                Set(
                  (diffHash, KeyModified(keyDiffHash.remoteKey, lastModified)))
              )(result)
            }
          })
      }
    }
  }

}
