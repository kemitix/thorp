package net.kemitix.s3thorp

import java.io.File

trait KeyGenerator {

  def generateKey(c: Config)(file: File): RemoteKey = {
    val otherPath = file.toPath.toAbsolutePath
    val sourcePath = c.source.toPath
    val relativePath = sourcePath.relativize(otherPath)
    RemoteKey(s"${c.prefix.key}/$relativePath")
  }

}
