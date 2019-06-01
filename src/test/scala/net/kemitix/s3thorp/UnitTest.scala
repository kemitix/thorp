package net.kemitix.s3thorp

import java.io.File

import org.scalatest.FunSpec

abstract class UnitTest extends FunSpec {

  def aLocalFile(path: String, myHash: MD5Hash, source: File, fileToKey: File => RemoteKey)
                (implicit c: Config): LocalFile =
    LocalFile(
      file = source.toPath.resolve(path).toFile,
      source = source,
      keyGenerator = fileToKey,
      suppliedHash = Some(myHash))

  def aRemoteKey(prefix: RemoteKey, path: String): RemoteKey =
    RemoteKey(prefix.key + "/" + path)

}
