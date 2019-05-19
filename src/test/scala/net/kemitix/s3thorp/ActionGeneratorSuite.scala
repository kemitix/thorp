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
      def invokeSubject(input: S3MetaData) = createActions(input).toList

      describe("#1 local exists, remote exists, remote matches - do nothing") {it("I should write this test"){pending}}
      describe("#2 local exists, remote is missing, other matches - copy") {
        val input = S3MetaData(localFile,
          matchByHash = Set(RemoteMetaData(otherKey, localHash, lastModified)),
          matchByKey = None)
        it("copy from other key") {
          assertResult(List(ToCopy(otherKey, localHash, localFile.remoteKey)))(invokeSubject(input))
        }
      }
      describe("#3 local exists, remote is missing, other no matches - upload") {
        val input = S3MetaData(localFile, Set.empty, None)
        it("upload") {
          assertResult(List(ToUpload(localFile)))(invokeSubject(input))
        }
      }
      describe("#4 local exists, remote exists, remote no match, other matches - copy") {
        val input = S3MetaData(localFile,
          matchByHash = Set(RemoteMetaData(otherKey, localHash, lastModified)),
          matchByKey = Some(RemoteMetaData(localFile.remoteKey, MD5Hash("previous-hash"), lastModified)))
        it("copy from other key") {
          assertResult(List(ToCopy(otherKey, localHash, localFile.remoteKey)))(invokeSubject(input))
        }
      }
      describe("#5 local exists, remote exists, remote no match, other no matches - upload") {it("I should write this test"){pending}}
      describe("#6 local missing, remote exists - delete") {it("I should write this test"){pending}}
    }
  }
}
