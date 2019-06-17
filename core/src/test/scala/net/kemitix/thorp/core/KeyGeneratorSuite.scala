package net.kemitix.thorp.core

import java.io.File

import net.kemitix.thorp.domain.{Bucket, Config, RemoteKey}
import org.scalatest.FunSpec

class KeyGeneratorSuite extends FunSpec {

    private val source: File = Resource(this, "upload")
    private val prefix = RemoteKey("prefix")
    implicit private val config: Config = Config(Bucket("bucket"), prefix, source = source)
    private val fileToKey = KeyGenerator.generateKey(config.source, config.prefix) _

    describe("key generator") {
      def resolve(subdir: String): File = {
        source.toPath.resolve(subdir).toFile
      }

      describe("when file is within source") {
        it("has a valid key") {
          val subdir = "subdir"
          assertResult(RemoteKey(s"${prefix.key}/$subdir"))(fileToKey(resolve(subdir)))
        }
      }

      describe("when file is deeper within source") {
        it("has a valid key") {
          val subdir = "subdir/deeper/still"
          assertResult(RemoteKey(s"${prefix.key}/$subdir"))(fileToKey(resolve(subdir)))
        }
      }
    }

}