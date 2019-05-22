package net.kemitix.s3thorp

import java.io.File

trait KeyGenerator {

  def generateKey(source: File, prefix: RemoteKey)
                 (file: File): RemoteKey = {
    val otherPath = file.toPath.toAbsolutePath
    val sourcePath = source.toPath
    val relativePath = sourcePath.relativize(otherPath)
    RemoteKey(s"${prefix.key}/$relativePath")
  }

}
