package net.kemitix.s3thorp

import java.io.File

import net.kemitix.s3thorp.domain.{MD5Hash, RemoteKey}
import org.scalatest.FunSpec

abstract class UnitTest extends FunSpec {

  def aLocalFile(path: String,
                 myHash: MD5Hash,
                 source: File,
                 fileToKey: File => RemoteKey,
                 fileToHash: File => MD5Hash): LocalFile =
    LocalFile(
      file = source.toPath.resolve(path).toFile,
      source = source,
      keyGenerator = fileToKey,
      md5HashGenerator = fileToHash,
      suppliedHash = Some(myHash))

  def aRemoteKey(prefix: RemoteKey, path: String): RemoteKey =
    RemoteKey(prefix.key + "/" + path)

}
