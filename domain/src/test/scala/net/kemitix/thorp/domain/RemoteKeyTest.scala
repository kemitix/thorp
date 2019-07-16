package net.kemitix.thorp.domain

import java.io.File
import java.nio.file.Paths

import org.scalatest.FreeSpec

class RemoteKeyTest extends FreeSpec {

  private val emptyKey = RemoteKey("")

  "create a RemoteKey" - {
    "can resolve a path" - {
      "when key is empty" in {
        val key      = emptyKey
        val path     = "path"
        val expected = RemoteKey("path")
        val result   = key.resolve(path)
        assertResult(expected)(result)
      }
      "when path is empty" in {
        val key      = RemoteKey("key")
        val path     = ""
        val expected = RemoteKey("key")
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
        val key      = RemoteKey("prefix/key")
        val source   = Paths.get("source")
        val prefix   = RemoteKey("prefix")
        val expected = Some(new File("source/key"))
        val result   = key.asFile(source, prefix)
        assertResult(expected)(result)
      }
      "when prefix is empty" in {
        val key      = RemoteKey("key")
        val source   = Paths.get("source")
        val prefix   = emptyKey
        val expected = Some(new File("source/key"))
        val result   = key.asFile(source, prefix)
        assertResult(expected)(result)
      }
      "when key is empty" in {
        val key      = emptyKey
        val source   = Paths.get("source")
        val prefix   = RemoteKey("prefix")
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
  }

}
