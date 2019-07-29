package net.kemitix.thorp

import java.io.File

import zio.ZIO

package object filesystem {
  final val fileSystemService: ZIO[FileSystem, Nothing, FileSystem.Service] =
    ZIO.access(_.filesystem)
  final def fileExists(file: File): ZIO[FileSystem, Throwable, Boolean] =
    ZIO.accessM(_.filesystem fileExists file)
}
