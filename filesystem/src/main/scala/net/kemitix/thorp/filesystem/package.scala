package net.kemitix.thorp

import zio.ZIO

package object filesystem {
  final val fileSystemService: ZIO[FileSystem, Nothing, FileSystem.Service] =
    ZIO.access(_.filesystem)
}
