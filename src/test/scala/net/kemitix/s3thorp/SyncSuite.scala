package net.kemitix.s3thorp

import java.time.Instant

import cats.effect.IO
import org.scalatest.FunSpec

class SyncSuite extends FunSpec {

  describe("s3client thunk") {
    val hash = "hash"
    val lastModified = Instant.now()
    val sync = new Sync(new S3Client {
      override def objectHead(bucket: String, key: String) = IO(Some((hash, lastModified)))
    })
    describe("objectHead") {
      it("return the hash and lastModified expected") {
        assertResult(Some((hash, lastModified)))(sync.objectHead("", "").unsafeRunSync())
      }
    }
  }
}
