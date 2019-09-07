package net.kemitix.thorp.lib

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console.Console
import net.kemitix.thorp.filesystem.{FileSystem, Hasher}
import net.kemitix.thorp.storage.Storage
import zio.ZIO
import zio.clock.Clock

object CoreTypes {

  type CoreEnv = Storage
    with Console
    with Config
    with Clock
    with FileSystem
    with Hasher
    with FileScanner
  type CoreProgram[A] = ZIO[CoreEnv, Throwable, A]

}