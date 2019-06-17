package net.kemitix.thorp.core

import java.io.File

import net.kemitix.thorp.domain.RemoteKey

object KeyGenerator {

  def generateKey(source: File, prefix: RemoteKey)
                 (file: File): RemoteKey = {
    val otherPath = file.toPath.toAbsolutePath
    val sourcePath = source.toPath
    val relativePath = sourcePath.relativize(otherPath)
    RemoteKey(s"${prefix.key}/$relativePath")
  }

}
