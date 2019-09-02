package net.kemitix.thorp.lib

import java.io.File

import net.kemitix.thorp.lib.Action._
import net.kemitix.thorp.domain.{
  Bucket,
  HashType,
  LocalFile,
  MD5Hash,
  RemoteKey
}
import org.scalatest.FreeSpec

class SequencePlanTest extends FreeSpec {

  "sort" - {
    "a list of assorted actions" - {
      val bucket     = Bucket("aBucket")
      val remoteKey1 = RemoteKey("remoteKey1")
      val remoteKey2 = RemoteKey("targetHash")
      val hash       = MD5Hash("aHash")
      val hashes     = Map[HashType, MD5Hash]()
      val size       = 1024
      val file1      = new File("aFile")
      val file2      = new File("aFile")
      val source     = new File("source")
      val localFile1 =
        LocalFile(file1, source, hashes, remoteKey1, file1.length)
      val _ =
        LocalFile(file2, source, hashes, remoteKey2, file2.length)
      val copy1   = ToCopy(bucket, remoteKey1, hash, remoteKey2, size)
      val copy2   = ToCopy(bucket, remoteKey2, hash, remoteKey1, size)
      val upload1 = ToUpload(bucket, localFile1, size)
      val upload2 = ToUpload(bucket, localFile1, size)
      val delete1 = ToDelete(bucket, remoteKey1, size)
      val delete2 = ToDelete(bucket, remoteKey2, size)
      "should be in correct order" in {
        val actions =
          List[Action](copy1, delete1, upload1, delete2, upload2, copy2)
        val expected =
          List[Action](copy1, copy2, upload1, upload2, delete1, delete2)
        val result = actions.sortBy(SequencePlan.order)
        assertResult(expected)(result)
      }
    }
  }

}
