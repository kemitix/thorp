package net.kemitix.thorp.core

import net.kemitix.thorp.domain.{MD5Hash, RemoteKey}
import net.kemitix.thorp.storage.api.StorageQueueEvent.{CopyQueueEvent, DeleteQueueEvent, UploadQueueEvent}
import org.scalatest.FunSpec

class StorageQueueEventSuite extends FunSpec {

  describe("Ordering of types") {
    val remoteKey = RemoteKey("remote-key")
    val md5Hash = MD5Hash("md5hash")
    val copy = CopyQueueEvent(remoteKey)
    val upload = UploadQueueEvent(remoteKey, md5Hash)
    val delete = DeleteQueueEvent(remoteKey)
    val unsorted = List(delete, copy, upload)
    it("should sort as copy < upload < delete ") {
      val result = unsorted.sorted
      val expected = List(copy, upload, delete)
      assertResult(expected)(result)
    }
  }

}
