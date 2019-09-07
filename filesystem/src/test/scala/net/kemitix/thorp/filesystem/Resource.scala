package net.kemitix.thorp.filesystem

import java.io.File

object Resource {

  def apply(
      base: AnyRef,
      name: String
  ): File = new File(base.getClass.getResource(name).getPath)
}
