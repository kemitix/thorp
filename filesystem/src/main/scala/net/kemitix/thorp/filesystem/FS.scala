package net.kemitix.thorp.filesystem

import java.io.{File, FileInputStream}

import zio.{TaskR, ZIO, ZManaged}

object FS {

  final def exists(file: File): ZIO[FileSystem, Throwable, Boolean] =
    ZIO.accessM(_.filesystem fileExists file)

  final def open(file: File, offset: Long = 0)
    : TaskR[FileSystem, ZManaged[FileSystem, Throwable, FileInputStream]] =
    ZIO.accessM(_.filesystem openManagedFileInputStream (file, offset))

  final def lines(file: File): TaskR[FileSystem, List[String]] =
    ZIO.accessM(_.filesystem fileLines (file))

}
