package net.kemitix.s3thorp.awssdk

import java.io.File

import cats.effect.IO
import net.kemitix.s3thorp.{Bucket, Config, KeyGenerator, MD5Hash, RemoteKey, Resource, UnitTest}
import software.amazon.awssdk.services.s3.model.{CompleteMultipartUploadRequest, CompleteMultipartUploadResponse, CreateMultipartUploadRequest, CreateMultipartUploadResponse, UploadPartRequest, UploadPartResponse}

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
      // small-file: dd if=/dev/urandom of=src/test/resources/net/kemitix/s3thorp/small-file bs=1047552 count=5
      // 1047552 = 1024 * 1023
      // file size 5kb under 5Mb threshold
      val smallFile = aLocalFile("small-file", MD5Hash(""), source, fileToKey)
      assert(smallFile.file.exists, "sample small file is missing")
      assert(smallFile.file.length == 5 * 1024 * 1023, "sample small file is wrong size")
      val result = uploader.accepts(smallFile)
      assertResult(false)(result)
    }
    it("should accept big file") {
      // big-file: dd if=/dev/urandom of=src/test/resources/net/kemitix/s3thorp/big-file bs=1049600 count=5
      // 1049600 = 1024 * 1025
      // file size 5kb over 5Mb threshold
      val bigFile = aLocalFile("big-file", MD5Hash(""), source, fileToKey)
      assert(bigFile.file.exists, "sample big file is missing")
      assert(bigFile.file.length == 5 * 1024 * 1025, "sample big file is wrong size")
      println(s"bigFile.file.length: ${bigFile.file.length}")
      val result = uploader.accepts(bigFile)
      assertResult(true)(result)
    }
  }

  describe("multi-part uploader upload") {
    val uploadId = "upload-id"
    val createUploadResponse = CreateMultipartUploadResponse.builder.uploadId(uploadId).build
    val uploadPartRequest0 = UploadPartRequest.builder.partNumber(0).build
    val uploadPartRequest1 = UploadPartRequest.builder.partNumber(1).build
    val uploadPartRequest2 = UploadPartRequest.builder.partNumber(2).build
    val uploadPartResponse0 = UploadPartResponse.builder.eTag("part-0").build
    val uploadPartResponse1 = UploadPartResponse.builder.eTag("part-1").build
    val uploadPartResponse2 = UploadPartResponse.builder.eTag("part-2").build
    val completeUploadResonse = CompleteMultipartUploadResponse.builder.build
    val uploader = new S3ClientMultiPartUploader(new MyS3CatsIOClient {
      override def createMultipartUpload(createMultipartUploadRequest: CreateMultipartUploadRequest): IO[CreateMultipartUploadResponse] =
        IO(createUploadResponse)

      override def uploadPartFromFile(uploadPartRequest: UploadPartRequest, sourceFile: File): IO[UploadPartResponse] =
        IO {
          uploadPartRequest match {
            case _ if uploadPartRequest.partNumber() == 0 => uploadPartResponse0
            case _ if uploadPartRequest.partNumber() == 1 => uploadPartResponse1
            case _ if uploadPartRequest.partNumber() == 2 => uploadPartResponse2
          }
        }

      override def completeMultipartUpload(completeMultipartUploadRequest: CompleteMultipartUploadRequest): IO[CompleteMultipartUploadResponse] =
        IO(completeUploadResonse)
    })
    val theFile = aLocalFile("big-file", MD5Hash(""), source, fileToKey)
    describe("initiate upload") {
      it("should createMultipartUpload") {
        val expected = createUploadResponse
        val result = uploader.createUpload(config.bucket, theFile).unsafeRunSync
        assertResult(expected)(result)
      }
    }
    describe("create UploadPartRequests for file") {
      val chunkSize = 5l * 1024 * 1025 / 2
      // to create expected md5 values for each chunK:
      // split -d -b $((5 * 1024 * 1025 / 2)) big-file
      // creates x00 and x01
      // md5sum x0[01]
      val part1md5 = "aadf0d266cefe0fcdb241a51798d74b3"
      val part2md5 = "16e08d53ca36e729d808fd5e4f7e35dc"
      val part0 = UploadPartRequest.builder
        .uploadId(uploadId)
        .partNumber(0)
        .contentLength(chunkSize)
        .contentMD5(part1md5)
        .build
      val part1 = UploadPartRequest.builder
        .uploadId(uploadId)
        .partNumber(1)
        .contentLength(chunkSize)
        .contentMD5(part2md5)
        .build
      it("should create the parts expected") {
        val result = uploader.parts(theFile, createUploadResponse).unsafeRunSync.toList
        assertResult(2)(result.size)
        assertResult(part1)(result(1))
        assertResult(part0)(result(0))
      }
    }
    describe("upload part") {
      it("should uploadPart") {
        val expected = uploadPartResponse2
        val result = uploader.uploadPart(theFile)(uploadPartRequest2).unsafeRunSync
        assertResult(expected)(result)
      }
    }
    describe("upload parts") {
      val uploadPartRequests = Stream(uploadPartRequest0, uploadPartRequest1)
      it("should uploadPart for each") {
        val expected = List(uploadPartResponse0, uploadPartResponse1)
        val result = uploader.uploadParts(theFile, uploadPartRequests).unsafeRunSync.toList
        assertResult(expected)(result)
      }
    }
    describe("complete upload") {
      val uploadPartResponses = Stream(uploadPartResponse0, uploadPartResponse1, uploadPartResponse2)
      it("should completeUpload") {
        val expected = completeUploadResonse
        val result = uploader.completeUpload(createUploadResponse, uploadPartResponses).unsafeRunSync
        assertResult(expected)(result)
      }
    }
  }
}
