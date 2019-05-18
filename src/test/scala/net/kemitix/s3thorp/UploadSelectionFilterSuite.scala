package net.kemitix.s3thorp

import java.io.File
import java.time.Instant

import org.scalatest.FunSpec

class UploadSelectionFilterSuite extends FunSpec {

  new UploadSelectionFilter {
    describe("uploadRequiredFilter") {
      val localFile = Resource(this, "test-file-for-hash.txt")
      val localHash = MD5Hash("0cbfe978783bd7950d5da4ff85e4af37")
      implicit val config: Config = Config(Bucket("bucket"), RemoteKey("prefix"), source = localFile.getParentFile)
      def invokeSubject(input: S3MetaData) =
        uploadRequiredFilter(input).toList
      describe("when supplied a file") {
        val input = S3MetaData(localFile, None)
        it("should be marked for upload") {
          assertResult(List(ToUpload(localFile)))(invokeSubject(input))
        }
      }
      describe("when supplied S3MetaData") {
        describe("when hash is different") {
          val input = S3MetaData(localFile, Some((RemoteKey(""), MD5Hash("doesn't match any hash"), LastModified(Instant.now))))
          it("should be marked for upload") {
            assertResult(List(ToUpload(localFile)))(invokeSubject(input))
          }
        }
        describe("when hash is the same") {
          val input = S3MetaData(localFile, Some((RemoteKey(""), localHash, LastModified(Instant.now))))
          it("should not be marked for upload") {
            assertResult(List())(invokeSubject(input))
          }
        }
      }
    }
  }
}
