package net.kemitix.thorp.core

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console.Console
import net.kemitix.thorp.filesystem.{FileSystem, Hasher}
import net.kemitix.thorp.storage.Storage
import zio.ZIO

object CoreTypes {

  type CoreEnv        = Storage with Console with Config with FileSystem with Hasher
  type CoreProgram[A] = ZIO[CoreEnv, Throwable, A]

}
