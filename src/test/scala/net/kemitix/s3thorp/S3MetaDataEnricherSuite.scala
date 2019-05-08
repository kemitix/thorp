package net.kemitix.s3thorp

import java.io.File
import java.nio.file.Paths

import org.scalatest.FunSpec

class S3MetaDataEnricherSuite extends FunSpec {

  new S3MetaDataEnricher {
    describe("key generator") {
      val path = "/root/from/here"
      val source = Paths.get(path).toFile
      val prefix = "prefix"
      val config = Config("bucket", prefix, source)
      val subject = generateKey(config)_

      def resolve(subdir: String): File = {
        source.toPath.resolve(subdir).toFile
      }

      describe("when file is within source") {
        it("has a valid key") {
          val subdir = "subdir"
          assertResult(s"$prefix/$subdir")(subject(resolve(subdir)))
        }
      }

      describe("when file is deeper within source") {
        it("has a valid key") {
          val subdir = "subdir/deeper/still"
          assertResult(s"$prefix/$subdir")(subject(resolve(subdir)))
        }
      }
    }
    override def objectHead(bucket: String, key: String) = ???
  }
}
