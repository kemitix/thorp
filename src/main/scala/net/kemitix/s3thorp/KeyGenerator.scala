package net.kemitix.s3thorp

import java.io.File

trait KeyGenerator {

  def generateKey(c: Config)(file: File): String = {
    val otherPath = file.toPath.toAbsolutePath
    val sourcePath = c.source.toPath
    val relativePath = sourcePath.relativize(otherPath)
    s"${c.prefix}/$relativePath"
  }

}
