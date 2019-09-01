package net.kemitix.thorp.storage.aws

import net.kemitix.thorp.config.Resource
import net.kemitix.thorp.core.{LocalFileValidator, S3MetaDataEnricher}
import net.kemitix.thorp.domain.HashType.MD5
import net.kemitix.thorp.domain._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSpec

import scala.collection.MapView

class StorageServiceSuite extends FunSpec with MockFactory {

  private val source     = Resource(this, "upload")
  private val sourcePath = source.toPath
  private val sources    = Sources(List(sourcePath))
  private val prefix     = RemoteKey("prefix")

  describe("getS3Status") {

    val hash = MD5Hash("hash")
    val env = for {
      localFile <- LocalFileValidator.resolve("the-file",
                                              Map(MD5 -> hash),
                                              sourcePath,
                                              sources,
                                              prefix)
      key = localFile.remoteKey
      keyOtherKey <- LocalFileValidator.resolve("other-key-same-hash",
                                                Map(MD5 -> hash),
                                                sourcePath,
                                                sources,
                                                prefix)
      diffHash = MD5Hash("diff")
      keyDiffHash <- LocalFileValidator.resolve("other-key-diff-hash",
                                                Map(MD5 -> diffHash),
                                                sourcePath,
                                                sources,
                                                prefix)
      s3ObjectsData = RemoteObjects(
        byHash = MapView(
          hash     -> Set(key, keyOtherKey.remoteKey),
          diffHash -> Set(keyDiffHash.remoteKey)
        ),
        byKey = MapView(
          key                   -> hash,
          keyOtherKey.remoteKey -> hash,
          keyDiffHash.remoteKey -> diffHash
        )
      )
    } yield
      (s3ObjectsData,
       localFile: LocalFile,
       keyOtherKey,
       keyDiffHash,
       diffHash,
       key)

    def invoke(localFile: LocalFile, s3ObjectsData: RemoteObjects) =
      S3MetaDataEnricher.getS3Status(localFile, s3ObjectsData)

    def getMatchesByKey(status: (Option[MD5Hash], Set[(RemoteKey, MD5Hash)]))
      : Option[MD5Hash] = {
      val (byKey, _) = status
      byKey
    }

    def getMatchesByHash(status: (Option[MD5Hash], Set[(RemoteKey, MD5Hash)]))
      : Set[(RemoteKey, MD5Hash)] = {
      val (_, byHash) = status
      byHash
    }

    describe(
      "when remote key exists, unmodified and other key matches the hash") {
      it("should return the match by key") {
        env.map({
          case (s3ObjectsData, localFile, _, _, _, _) => {
            val result = getMatchesByKey(invoke(localFile, s3ObjectsData))
            assert(result.contains(hash))
          }
        })
      }
      it("should return both matches for the hash") {
        env.map({
          case (s3ObjectsData, localFile, keyOtherKey, _, _, key) => {
            val result = getMatchesByHash(invoke(localFile, s3ObjectsData))
            assertResult(
              Set((hash, key), (hash, keyOtherKey.remoteKey))
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
                 sources,
                 prefix)
      it("should return no matches by key") {
        env2.map(localFile => {
          env.map({
            case (s3ObjectsData, _, _, _, _, _) => {
              val result = getMatchesByKey(invoke(localFile, s3ObjectsData))
              assert(result.isEmpty)
            }
          })
        })
      }
      it("should return no matches by hash") {
        env2.map(localFile => {
          env.map({
            case (s3ObjectsData, _, _, _, _, _) => {
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
          case (s3ObjectsData, _, _, keyDiffHash, diffHash, _) => {
            val result = getMatchesByKey(invoke(keyDiffHash, s3ObjectsData))
            assert(result.contains(diffHash))
          }
        })
      }
      it("should return one match by hash") {
        env.map({
          case (s3ObjectsData, _, _, keyDiffHash, diffHash, _) => {
            val result = getMatchesByHash(invoke(keyDiffHash, s3ObjectsData))
            assertResult(
              Set((diffHash, keyDiffHash.remoteKey))
            )(result)
          }
        })
      }
    }
  }

}
