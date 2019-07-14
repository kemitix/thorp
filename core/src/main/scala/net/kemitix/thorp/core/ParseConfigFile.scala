package net.kemitix.thorp.core

import java.nio.file.{Files, Path}

import cats.effect.IO

import scala.collection.JavaConverters._

trait ParseConfigFile {

  def parseFile(filename: Path): IO[ConfigOptions] =
    readFile(filename).map(ParseConfigLines.parseLines)

  private def readFile(filename: Path) = {
    if (Files.exists(filename)) readFileThatExists(filename)
    else IO.pure(List())
  }

  private def readFileThatExists(filename: Path) =
    for {
      lines <- IO(Files.lines(filename))
      list = lines.iterator.asScala.toList
      _ <- IO(println(s"lines as list: $list"))
      _ <- IO(lines.close())
    } yield list

}

object ParseConfigFile extends ParseConfigFile