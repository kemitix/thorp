package net.kemitix.s3thorp

import java.io.File

import cats.effect.IO
import net.kemitix.s3thorp.Sync.{Bucket, LocalFile, RemoteKey}
import org.scalatest.FunSpec

class S3UploaderSuite extends FunSpec {

  new S3Uploader {
    val md5Hash = "the-md5hash"
    override def objectHead(bucket: String, key: String) = ???
    override def upload(localFile: LocalFile, bucket: Bucket, remoteKey: RemoteKey) =
      IO(Right(md5Hash))

    describe("upload") {
      val config: Config = Config("bucket", "prefix", new File("/path/to/files"))
      def invoke(file: File) =
        performUpload(config)(file).compile.toList.unsafeRunSync()
      it("should return") {
        val result = invoke(new File("/path/to/files/a-file-to-upload.txt"))
        assertResult(List(()))(result)
      }
    }

  }
}
