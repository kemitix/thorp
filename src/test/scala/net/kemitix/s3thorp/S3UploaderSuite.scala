package net.kemitix.s3thorp

import java.io.File

import cats.effect.IO
import net.kemitix.s3thorp.Sync.{Bucket, LastModified, LocalFile, MD5Hash, RemoteKey}
import org.scalatest.FunSpec

class S3UploaderSuite extends FunSpec {

  new S3Uploader {
    override def objectHead(bucket: String, key: String): IO[Option[(MD5Hash, LastModified)]] = ???
    override def upload(localFile: LocalFile, bucket: Bucket, remoteKey: RemoteKey): IO[Unit] = IO()

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
