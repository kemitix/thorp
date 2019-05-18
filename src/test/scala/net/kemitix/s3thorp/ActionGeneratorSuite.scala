package net.kemitix.s3thorp

import java.time.Instant

import org.scalatest.FunSpec

class ActionGeneratorSuite
  extends FunSpec
    with KeyGenerator {

  new ActionGenerator {
    describe("create actions") {
      val resource = Resource(this, "test-file-for-hash.txt")
      val localHash = MD5Hash("0cbfe978783bd7950d5da4ff85e4af37")
      implicit val config: Config = Config(Bucket("bucket"), RemoteKey("prefix"), source = resource.getParentFile)
      val fileToKey = generateKey(config.source, config.prefix) _
      val localFile = LocalFile(resource, config.source, fileToKey)
      val lastModified = LastModified(Instant.now)
      val otherKey = RemoteKey("prefix/other-file")
      def invokeSubject(input: S3MetaData) =
        createActions(input).toList
      describe("#1 remote key exists, hash dpes not match, hash of other keys match") {
        val input = S3MetaData(localFile,
          matchByHash = Set(RemoteMetaData(otherKey, localHash, lastModified)),
          matchByKey = Some(RemoteMetaData(localFile.remoteKey, MD5Hash("previous-hash"), lastModified)))
        it("copy from other key") {
          assertResult(List(ToCopy(otherKey, localHash, localFile.remoteKey)))(invokeSubject(input))
        }
      }
      describe("#5 remote key is missing, hash of other keys match") {
        val input = S3MetaData(localFile,
          matchByHash = Set(RemoteMetaData(otherKey, localHash, lastModified)),
          matchByKey = None)
        it("copy from other key") {
          assertResult(List(ToCopy(otherKey, localHash, localFile.remoteKey)))(invokeSubject(input))
        }
      }
      describe("#6 remote key is missing, hash of other keys do not match") {
        val input = S3MetaData(localFile, Set.empty, None)
        it("upload") {
          assertResult(List(ToUpload(localFile)))(invokeSubject(input))
        }
      }
    }
  }
}
