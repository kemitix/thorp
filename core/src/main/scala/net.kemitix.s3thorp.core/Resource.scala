package net.kemitix.s3thorp.core

import java.io.{File, FileNotFoundException}

import scala.util.Try

object Resource {

  def apply(base: AnyRef,
            name: String): File = {
    Try{
      println(s"\nname = ${name}")
      val clazz = base.getClass
      println(s"clazz = ${clazz}")
      val resource = clazz.getResource(name)
      println(s"resource = ${resource}")
      val path = resource.getPath
      println(s"path = ${path}\n")
      new File(path)
    }.getOrElse(throw new FileNotFoundException(name))
  }
}
