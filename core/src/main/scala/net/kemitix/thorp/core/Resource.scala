package net.kemitix.thorp.core

import java.io.{File, FileNotFoundException}

import scala.util.Try

object Resource {

  def apply(
      base: AnyRef,
      name: String
  ): File =
    Try {
      new File(base.getClass.getResource(name).getPath)
    }.getOrElse(throw new FileNotFoundException(name))
}
