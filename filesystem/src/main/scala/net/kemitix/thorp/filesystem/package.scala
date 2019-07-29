package net.kemitix.thorp

import java.io.{File, FileInputStream}

import zio.{TaskR, ZIO, ZManaged}

package object filesystem {
  final val fileSystemService: ZIO[FileSystem, Nothing, FileSystem.Service] =
    ZIO.access(_.filesystem)
  final def fileExists(file: File): ZIO[FileSystem, Throwable, Boolean] =
    ZIO.accessM(_.filesystem fileExists file)
  def openFile(file: File, offset: Long)
    : TaskR[FileSystem, ZManaged[FileSystem, Throwable, FileInputStream]] =
    ZIO.accessM(_.filesystem openManagedFileInputStream (file, offset))
}
