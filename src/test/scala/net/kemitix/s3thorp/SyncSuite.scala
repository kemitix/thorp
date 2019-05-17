package net.kemitix.s3thorp

import java.io.File
import java.time.Instant

import cats.effect.IO
import net.kemitix.s3thorp.awssdk.{HashLookup, S3Client}
import org.scalatest.FunSpec

class SyncSuite extends FunSpec {

  describe("s3client thunk") {
    val testBucket = Bucket("bucket")
    val testRemoteKey = RemoteKey("prefix/file")
    describe("upload") {
      val md5Hash = MD5Hash("the-hash")
      val testLocalFile = new File("file")
      val sync = new Sync(new S3Client with DummyS3Client {
        override def upload(localFile: File, bucket: Bucket, remoteKey: RemoteKey): IO[Either[Throwable, MD5Hash]] = {
          assert(localFile == testLocalFile)
          assert(bucket == testBucket)
          assert(remoteKey == testRemoteKey)
          IO(Right(md5Hash))
        }
      })
      it("delegates unmodified to the S3Client") {
        assertResult(Right(md5Hash))(
          sync.upload(testLocalFile, testBucket, testRemoteKey).
            unsafeRunSync())
      }
    }
  }
  describe("run") {
    val testBucket = Bucket("bucket")
    val source = Resource(this, "upload")
    // source contains the files root-file and subdir/leaf-file
    val config = Config(Bucket("bucket"), RemoteKey("prefix"), source = source)
    describe("when all files should be uploaded") {
      var uploadsRecord: Map[String, RemoteKey] = Map()
      var copiesRecord: Map[RemoteKey, RemoteKey] = Map()
      var deletionsRecord: Set[RemoteKey] = Set()
      val sync = new Sync(new DummyS3Client{
        override def listObjects(bucket: Bucket, prefix: RemoteKey) = IO(
          HashLookup(
            byHash = Map(),
            byKey = Map()))
        override def upload(localFile: File,
                            bucket: Bucket,
                            remoteKey: RemoteKey
                           ) = IO {
          if (bucket == testBucket)
            uploadsRecord += (source.toPath.relativize(localFile.toPath).toString -> remoteKey)
          Right(MD5Hash("some hash value"))
        }
        override def copy(bucket: Bucket,
                          sourceKey: RemoteKey,
                          hash: MD5Hash,
                          targetKey: RemoteKey
                         ) = {
          if (bucket == testBucket)
            copiesRecord += (sourceKey -> targetKey)
          IO(Right(targetKey))
        }
        override def delete(bucket: Bucket,
                            remoteKey: RemoteKey
                           ) = {
          if (bucket == testBucket)
            deletionsRecord += remoteKey
          IO(Right(remoteKey))
        }
      })
      sync.run(config).unsafeRunSync
      it("uploads all files") {
        val expectedUploads = Map(
          "subdir/leaf-file" -> RemoteKey("prefix/subdir/leaf-file"),
          "root-file" -> RemoteKey("prefix/root-file")
        )
        assertResult(expectedUploads)(uploadsRecord)
      }
      it("copies nothing") {
        val expectedCopies = Map()
        assertResult(expectedCopies)(copiesRecord)
      }
      it("deletes nothing") {
        val expectedDeletions = Set()
        assertResult(expectedDeletions)(deletionsRecord)
      }
    }
    describe("when no files should be uploaded") {
      val rootHash = MD5Hash("a3a6ac11a0eb577b81b3bb5c95cc8a6e")
      val leafHash = MD5Hash("208386a650bdec61cfcd7bd8dcb6b542")
      val lastModified = LastModified(Instant.now)
      var uploadsRecord: Map[String, RemoteKey] = Map()
      var copiesRecord: Map[RemoteKey, RemoteKey] = Map()
      var deletionsRecord: Set[RemoteKey] = Set()
      val sync = new Sync(new S3Client with DummyS3Client {
        override def listObjects(bucket: Bucket,
                                 prefix: RemoteKey
                                ) = IO(
          HashLookup(
            byHash = Map(
              rootHash -> (RemoteKey("prefix/root-file"), lastModified),
              leafHash -> (RemoteKey("prefix/subdir/leaf-file"), lastModified)),
            byKey = Map(
              RemoteKey("prefix/root-file") -> (rootHash, lastModified),
              RemoteKey("prefix/subdir/leaf-file") -> (leafHash, lastModified))))
        override def upload(localFile: File,
                            bucket: Bucket,
                            remoteKey: RemoteKey
                           ) = IO {
          if (bucket == testBucket)
            uploadsRecord += (source.toPath.relativize(localFile.toPath).toString -> remoteKey)
          Right(MD5Hash("some hash value"))
        }
        override def copy(bucket: Bucket,
                          sourceKey: RemoteKey,
                          hash: MD5Hash,
                          targetKey: RemoteKey
                         ): IO[Either[Throwable, RemoteKey]] = IO {
          if (bucket == testBucket)
            copiesRecord += (sourceKey -> targetKey)
          Right(targetKey)
        }
        override def delete(bucket: Bucket,
                            remoteKey: RemoteKey
                           ) = IO {
          if (bucket == testBucket)
            deletionsRecord += remoteKey
          Right(remoteKey)
        }
      })
      sync.run(config).unsafeRunSync
      it("uploads nothing") {
        val expectedUploads = Map()
        assertResult(expectedUploads)(uploadsRecord)
      }
      it("copies nothing") {
        val expectedCopies = Map()
        assertResult(expectedCopies)(copiesRecord)
      }
      it("deletes nothing") {
        val expectedDeletions = Set()
        assertResult(expectedDeletions)(deletionsRecord)
      }
    }
    describe("when a file is renamed it is moved on S3 with no upload") {
      // 'root-file-old' should be renamed as 'root-file'
      val rootHash = MD5Hash("a3a6ac11a0eb577b81b3bb5c95cc8a6e")
      val leafHash = MD5Hash("208386a650bdec61cfcd7bd8dcb6b542")
      val lastModified = LastModified(Instant.now)
      var uploadsRecord: Map[String, RemoteKey] = Map()
      var copiesRecord: Map[RemoteKey, RemoteKey] = Map()
      var deletionsRecord: Set[RemoteKey] = Set()
      val sync = new Sync(new S3Client with DummyS3Client {
        override def listObjects(bucket: Bucket,
                                 prefix: RemoteKey
                                ) = IO {
          HashLookup(
            byHash = Map(
              rootHash -> (RemoteKey("prefix/root-file-old"), lastModified),
              leafHash -> (RemoteKey("prefix/subdir/leaf-file"), lastModified)),
            byKey = Map(
              RemoteKey("prefix/root-file-old") -> (rootHash, lastModified),
              RemoteKey("prefix/subdir/leaf-file") -> (leafHash, lastModified)))}
        override def upload(localFile: File,
                            bucket: Bucket,
                            remoteKey: RemoteKey
                           ) = IO {
          if (bucket == testBucket)
            uploadsRecord += (source.toPath.relativize(localFile.toPath).toString -> remoteKey)
          Right(MD5Hash("some hash value"))
        }
        override def copy(bucket: Bucket,
                          sourceKey: RemoteKey,
                          hash: MD5Hash,
                          targetKey: RemoteKey
                         ) = IO {
          if (bucket == testBucket)
            copiesRecord += (sourceKey -> targetKey)
          Right(targetKey)
        }
        override def delete(bucket: Bucket,
                            remoteKey: RemoteKey
                           ) = IO {
          if (bucket == testBucket)
            deletionsRecord += remoteKey
          Right(remoteKey)
        }
      })
      sync.run(config).unsafeRunSync
      it("uploads nothing") {
        pending
        val expectedUploads = Map()
        assertResult(expectedUploads)(uploadsRecord)
      }
      it("copies the file") {
        pending
        val expectedCopies = Map("prefix/root-file-old" -> "prefix/root-file")
        assertResult(expectedCopies)(copiesRecord)
      }
      it("deletes the original") {
        pending
        val expectedDeletions = Set("prefix/root-file-old")
        assertResult(expectedDeletions)(deletionsRecord)
      }
    }
    describe("when a file is copied it is copied on S3 with no upload") {}
  }
}
