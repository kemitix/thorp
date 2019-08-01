package net.kemitix.thorp.config

import java.nio.file.Path

import net.kemitix.thorp.filesystem.FileSystem
import zio.{IO, TaskR, ZIO}

trait ParseConfigFile {

  def parseFile(
      filename: Path): ZIO[FileSystem, Seq[ConfigValidation], ConfigOptions] =
    (readFile(filename) >>= ParseConfigLines.parseLines)
      .catchAll(
        h =>
          IO.fail(
            List(ConfigValidation.ErrorReadingFile(filename, h.getMessage))))

  private def readFile(filename: Path) =
    FileSystem.exists(filename.toFile) >>= readLines(filename)

  private def readLines(filename: Path)(
      exists: Boolean): TaskR[FileSystem, Seq[String]] =
    if (exists) FileSystem.lines(filename.toFile)
    else ZIO.succeed(Seq.empty)

}

object ParseConfigFile extends ParseConfigFile
