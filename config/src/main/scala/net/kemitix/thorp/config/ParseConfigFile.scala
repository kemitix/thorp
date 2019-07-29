package net.kemitix.thorp.config

import java.nio.file.{Files, Path}

import net.kemitix.thorp.filesystem._
import zio.{IO, ZIO}

trait ParseConfigFile {

  def parseFile(
      filename: Path): ZIO[FileSystem, List[ConfigValidation], ConfigOptions] =
    readFile(filename)
      .map(ParseConfigLines.parseLines)
      .catchAll(h =>
        IO.fail(
          List(ConfigValidation.ErrorReadingFile(filename, h.getMessage))))

  private def readFile(filename: Path) = {
    if (Files.exists(filename)) readFileThatExists(filename)
    else ZIO.succeed(List.empty)
  }

  private def readFileThatExists(filename: Path) =
    fileLines(filename.toFile)

}

object ParseConfigFile extends ParseConfigFile
