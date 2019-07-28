package net.kemitix.thorp.core

import net.kemitix.thorp.console.Console
import net.kemitix.thorp.storage.api.Storage
import zio.ZIO

object CoreTypes {

  type CoreEnv        = Storage with Console
  type CoreProgram[A] = ZIO[CoreEnv, Throwable, A]

}
