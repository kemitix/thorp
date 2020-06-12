package net.kemitix.thorp.domain

import java.io.File
import java.nio.file.Paths

import org.scalatest.FreeSpec
import zio.{DefaultRuntime, UIO}

class RemoteKeyTest extends FreeSpec {

  private val emptyKey = RemoteKey.create("")

  "create a RemoteKey" - {
    "can resolve a path" - {
      "when key is empty" in {
        val key      = emptyKey
        val path     = "path"
        val expected = RemoteKey.create("path")
        val result   = key.resolve(path)
        assertResult(expected)(result)
      }
      "when path is empty" in {
        val key      = RemoteKey.create("key")
        val path     = ""
        val expected = RemoteKey.create("key")
        val result   = key.resolve(path)
        assertResult(expected)(result)
      }
      "when key and path are empty" in {
        val key      = emptyKey
        val path     = ""
        val expected = emptyKey
        val result   = key.resolve(path)
        assertResult(expected)(result)
      }
    }
    "asFile" - {
      "when key and prefix are non-empty" in {
        val key      = RemoteKey.create("prefix/key")
        val source   = Paths.get("source")
        val prefix   = RemoteKey.create("prefix")
        val expected = Some(new File("source/key"))
        val result   = key.asFile(source, prefix)
        assertResult(expected)(result)
      }
      "when prefix is empty" in {
        val key      = RemoteKey.create("key")
        val source   = Paths.get("source")
        val prefix   = emptyKey
        val expected = Some(new File("source/key"))
        val result   = key.asFile(source, prefix)
        assertResult(expected)(result)
      }
      "when key is empty" in {
        val key      = emptyKey
        val source   = Paths.get("source")
        val prefix   = RemoteKey.create("prefix")
        val expected = None
        val result   = key.asFile(source, prefix)
        assertResult(expected)(result)
      }
      "when key and prefix are empty" in {
        val key      = emptyKey
        val source   = Paths.get("source")
        val prefix   = emptyKey
        val expected = None
        val result   = key.asFile(source, prefix)
        assertResult(expected)(result)
      }
    }
    "fromSourcePath" - {
      "when path in source" in {
        val source   = Paths.get("/source")
        val path     = source.resolve("/source/child")
        val expected = RemoteKey.create("child")
        val result   = RemoteKey.fromSourcePath(source, path)
        assertResult(expected)(result)
      }
    }
    "from source, prefix, file" - {
      "when file in source" in {
        val source   = Paths.get("/source")
        val prefix   = RemoteKey.create("prefix")
        val file     = new File("/source/dir/filename")
        val expected = RemoteKey.create("prefix/dir/filename")
        val program  = UIO(RemoteKey.from(source, prefix, file))
        val result   = new DefaultRuntime {}.unsafeRunSync(program).toEither
        assertResult(Right(expected))(result)
      }
    }
  }
  "asFile" - {
    "remoteKey is empty" in {
      val source    = Paths.get("/source")
      val prefix    = RemoteKey.create("prefix")
      val remoteKey = RemoteKey.create("")

      val expected = None

      val result = remoteKey.asFile(source, prefix)

      assertResult(expected)(result)
    }
    "remoteKey is not empty" - {
      "remoteKey is within prefix" in {
        val source    = Paths.get("/source")
        val prefix    = RemoteKey.create("prefix")
        val remoteKey = RemoteKey.create("prefix/key")

        val expected = Some(Paths.get("/source/key").toFile)

        val result = remoteKey.asFile(source, prefix)

        assertResult(expected)(result)
      }
      "remoteKey is outwith prefix" in {
        val source    = Paths.get("/source")
        val prefix    = RemoteKey.create("prefix")
        val remoteKey = RemoteKey.create("elsewhere/key")

        val expected = None

        val result = remoteKey.asFile(source, prefix)

        assertResult(expected)(result)
      }
    }
  }

}
