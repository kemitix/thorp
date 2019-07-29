package net.kemitix.thorp.config

import java.nio.file.Path

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

  private def readFile(filename: Path) =
    fileExists(filename.toFile)
      .flatMap(
        if (_) fileLines(filename.toFile)
        else ZIO.succeed(List.empty))

}

object ParseConfigFile extends ParseConfigFile
