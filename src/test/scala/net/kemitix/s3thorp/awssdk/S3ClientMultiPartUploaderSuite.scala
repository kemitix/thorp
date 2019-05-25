package net.kemitix.s3thorp.awssdk

import java.io.File
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}

import cats.effect.IO
import net.kemitix.s3thorp._
import software.amazon.awssdk.services.s3.model.{CompleteMultipartUploadRequest, CompleteMultipartUploadResponse, CreateMultipartUploadRequest, CreateMultipartUploadResponse, UploadPartRequest, UploadPartResponse}

class S3ClientMultiPartUploaderSuite
  extends UnitTest
    with KeyGenerator {

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

  describe("mulit-part uploader upload") {
    val theFile = aLocalFile("big-file", MD5Hash(""), source, fileToKey)
    val uploadId = "upload-id"
    val createUploadResponse = CreateMultipartUploadResponse.builder.uploadId(uploadId).build
    val uploadPartRequest0 = UploadPartRequest.builder.partNumber(0).build
    val uploadPartRequest1 = UploadPartRequest.builder.partNumber(1).build
    val uploadPartRequest2 = UploadPartRequest.builder.partNumber(2).build
    val uploadPartResponse0 = UploadPartResponse.builder.eTag("part-0").build
    val uploadPartResponse1 = UploadPartResponse.builder.eTag("part-1").build
    val uploadPartResponse2 = UploadPartResponse.builder.eTag("part-2").build
    val completeUploadResponse = CompleteMultipartUploadResponse.builder.eTag("hash").build
    describe("multi-part uploader upload components") {
      val uploader = new RecordingMultiPartUploader()
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
          val expected = completeUploadResponse
          val result = uploader.completeUpload(createUploadResponse, uploadPartResponses).unsafeRunSync
          assertResult(expected)(result)
        }
      }
    }
    describe("multi-part uploader upload complete") {
      describe("upload") {
        describe("when all okay") {
          val uploader = new RecordingMultiPartUploader()
          uploader.upload(theFile, config.bucket).unsafeRunSync
          it("should initiate the upload") {
            assert(uploader.initiated.get)
          }
          it("should upload both parts") {
            assertResult(Set(0, 1))(uploader.partsUploaded.get)
          }
          it("should complete the upload") {
            assert(uploader.completed.get)
          }
        }
        describe("when initiate upload fails") {
          it("should not upload any parts") {
            pending
          }
          it("should not complete the upload") {
            pending
          }
        }
        describe("when uploading a part fails once") {
          it("should retry the upload") {
            pending
          }
          it("should upload all parts") {
            pending
          }
          it("should complete the upload") {
            pending
          }
        }
        describe("when uploading a part fails too many times") {
          it("should retry the upload") {
            pending
          }
          it("should not complete the upload") {
            pending
          }
          it("should cancel the upload") {
            pending
          }
        }
      }
    }
    class RecordingMultiPartUploader(initOkay: Boolean = true,
                                     partTriesRequired: Int = 1,
                                     val initiated: AtomicBoolean = new AtomicBoolean(false),
                                     val partsUploaded: AtomicReference[Set[Int]] = new AtomicReference[Set[Int]](Set()),
                                     val part0Tries: AtomicInteger = new AtomicInteger(0),
                                     val part1Tries: AtomicInteger = new AtomicInteger(0),
                                     val part2Tries: AtomicInteger = new AtomicInteger(0),
                                     var completed: AtomicBoolean = new AtomicBoolean(false))
      extends S3ClientMultiPartUploader(
        new MyS3CatsIOClient {

          override def createMultipartUpload(createMultipartUploadRequest: CreateMultipartUploadRequest): IO[CreateMultipartUploadResponse] =
            if (initOkay) {
              initiated.set(true)
              IO {
                createUploadResponse
              }
            }
            else IO.raiseError(new Exception)

          override def uploadPartFromFile(uploadPartRequest: UploadPartRequest, sourceFile: File): IO[UploadPartResponse] =
            uploadPartRequest match {
              case _ if uploadPartRequest.partNumber == 0 => {
                if (part0Tries.incrementAndGet >= partTriesRequired) IO {
                  partsUploaded.getAndUpdate(t => t + 0)
                  uploadPartResponse0
                }
                else {
                  IO.raiseError(new Exception)
                }
              }
              case _ if uploadPartRequest.partNumber == 1 => {
                if (part1Tries.incrementAndGet >= partTriesRequired) IO {
                  partsUploaded.getAndUpdate(t => t + 1)
                  uploadPartResponse1
                }
                else {
                  IO.raiseError(new Exception)
                }
              }
              case _ if uploadPartRequest.partNumber == 2 => {
                if (part2Tries.incrementAndGet >= partTriesRequired) IO {
                  partsUploaded.getAndUpdate(t => t + 2)
                  uploadPartResponse2
                }
                else {
                  IO.raiseError(new Exception)
                }
              }
            }

          override def completeMultipartUpload(completeMultipartUploadRequest: CompleteMultipartUploadRequest): IO[CompleteMultipartUploadResponse] = {
            completed.set(true)
            IO {
              completeUploadResponse
            }
          }
        }) {}
  }
}