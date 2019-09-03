package net.kemitix.thorp.domain

import java.io.File
import java.nio.file.Paths

import org.scalatest.FreeSpec
import zio.DefaultRuntime

class RemoteKeyTest extends FreeSpec {

  private val emptyKey = RemoteKey("")

  "create a RemoteKey" - {
    "can resolve a path" - {
      "when key is empty" in {
        val key      = emptyKey
        val path     = "path"
        val expected = RemoteKey("path")
        val result   = RemoteKey.resolve(path)(key)
        assertResult(expected)(result)
      }
      "when path is empty" in {
        val key      = RemoteKey("key")
        val path     = ""
        val expected = RemoteKey("key")
        val result   = RemoteKey.resolve(path)(key)
        assertResult(expected)(result)
      }
      "when key and path are empty" in {
        val key      = emptyKey
        val path     = ""
        val expected = emptyKey
        val result   = RemoteKey.resolve(path)(key)
        assertResult(expected)(result)
      }
    }
    "asFile" - {
      "when key and prefix are non-empty" in {
        val key      = RemoteKey("prefix/key")
        val source   = Paths.get("source")
        val prefix   = RemoteKey("prefix")
        val expected = Some(new File("source/key"))
        val result   = RemoteKey.asFile(source, prefix)(key)
        assertResult(expected)(result)
      }
      "when prefix is empty" in {
        val key      = RemoteKey("key")
        val source   = Paths.get("source")
        val prefix   = emptyKey
        val expected = Some(new File("source/key"))
        val result   = RemoteKey.asFile(source, prefix)(key)
        assertResult(expected)(result)
      }
      "when key is empty" in {
        val key      = emptyKey
        val source   = Paths.get("source")
        val prefix   = RemoteKey("prefix")
        val expected = None
        val result   = RemoteKey.asFile(source, prefix)(key)
        assertResult(expected)(result)
      }
      "when key and prefix are empty" in {
        val key      = emptyKey
        val source   = Paths.get("source")
        val prefix   = emptyKey
        val expected = None
        val result   = RemoteKey.asFile(source, prefix)(key)
        assertResult(expected)(result)
      }
    }
    "fromSourcePath" - {
      "when path in source" in {
        val source   = Paths.get("/source")
        val path     = source.resolve("/source/child")
        val expected = RemoteKey("child")
        val result   = RemoteKey.fromSourcePath(source, path)
        assertResult(expected)(result)
      }
    }
    "from source, prefix, file" - {
      "when file in source" in {
        val source   = Paths.get("/source")
        val prefix   = RemoteKey("prefix")
        val file     = new File("/source/dir/filename")
        val expected = RemoteKey("prefix/dir/filename")
        val program  = RemoteKey.from(source, prefix, file)
        val result   = new DefaultRuntime {}.unsafeRunSync(program).toEither
        assertResult(Right(expected))(result)
      }
    }
  }

}
