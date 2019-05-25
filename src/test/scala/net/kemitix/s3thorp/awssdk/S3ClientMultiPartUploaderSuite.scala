package net.kemitix.s3thorp.awssdk

import net.kemitix.s3thorp.{Bucket, Config, KeyGenerator, MD5Hash, RemoteKey, Resource, UnitTest}

class S3ClientMultiPartUploaderSuite
  extends UnitTest
    with KeyGenerator{

  private val source = Resource(this, "..")
  private val prefix = RemoteKey("prefix")
  implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)
  private val fileToKey = generateKey(config.source, config.prefix) _

  describe("multi-part uploader accepts") {
    val uploader = new S3ClientMultiPartUploader(new MyS3CatsIOClient {})

    it("should reject small file") {
      val smallFile = aLocalFile("small-file", MD5Hash(""), source, fileToKey)
      assert(smallFile.file.exists, "sample small file is missing")
      // 1047552 * 5
      assert(smallFile.file.length == 5 * 1024 * 1023, "sample small file is wrong size")
      val result = uploader.accepts(smallFile)
      assertResult(false)(result)
    }
    it("should accept big file") {
      val bigFile = aLocalFile("big-file", MD5Hash(""), source, fileToKey)
      assert(bigFile.file.exists, "sample big file is missing")
      // 1049600  * 5 == big
      assert(bigFile.file.length == 5 * 1024 * 1025, "sample big file is wrong size")
      println(s"bigFile.file.length: ${bigFile.file.length}")
      val result = uploader.accepts(bigFile)
      assertResult(true)(result)
    }
  }
}
