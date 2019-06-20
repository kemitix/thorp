package net.kemitix.thorp.core

import java.nio.file.{Files, Path}

import cats.effect.IO

import scala.collection.JavaConverters._

trait ParseConfigFile {

  def apply(filename: Path): IO[Seq[ConfigOption]] =
    readFile(filename).map(ParseConfigLines(_))

  private def readFile(filename: Path) =
    IO {
      if (Files.exists(filename)) {
        val lines = Files.lines(filename)
        val scala = lines.iterator.asScala.toList
        lines.close()
        scala
      } else
        List()
    }

}

object ParseConfigFile extends ParseConfigFile