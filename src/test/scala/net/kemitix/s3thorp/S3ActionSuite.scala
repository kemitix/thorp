package net.kemitix.s3thorp

class S3ActionSuite extends UnitTest {

  describe("Ordering of types") {
    val remoteKey = RemoteKey("remote-key")
    val md5Hash = MD5Hash("md5hash")
    val copy = CopyS3Action(remoteKey)
    val upload = UploadS3Action(remoteKey, md5Hash)
    val delete = DeleteS3Action(remoteKey)
    val unsorted = List(delete, copy, upload)
    it("should sort as copy < upload < delete ") {
      val result = unsorted.sorted
      val expected = List(copy, upload, delete)
      assertResult(expected)(result)
    }
  }

}
