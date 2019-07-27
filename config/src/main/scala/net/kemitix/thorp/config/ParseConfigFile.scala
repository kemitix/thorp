package net.kemitix.thorp.config

import java.nio.file.{Files, Path}

import zio.{IO, Task}

import scala.collection.JavaConverters._

trait ParseConfigFile {

  def parseFile(filename: Path): IO[List[ConfigValidation], ConfigOptions] =
    readFile(filename)
      .map(ParseConfigLines.parseLines)
      .catchAll(h =>
        IO.fail(
          List(ConfigValidation.ErrorReadingFile(filename, h.getMessage))))

  private def readFile(filename: Path): Task[List[String]] = {
    if (Files.exists(filename)) readFileThatExists(filename)
    else IO(List())
  }

  private def readFileThatExists(filename: Path): Task[List[String]] =
    for {
      lines <- IO(Files.lines(filename))
      list = lines.iterator.asScala.toList
      //FIXME: use a bracket to close the file
      _ <- IO(lines.close())
    } yield list

}

object ParseConfigFile extends ParseConfigFile
