package net.kemitix.thorp.config

import java.io.File

import net.kemitix.thorp.filesystem.FileSystem
import zio.{IO, RIO, ZIO}

trait ParseConfigFile {

  def parseFile(
      file: File): ZIO[FileSystem, Seq[ConfigValidation], ConfigOptions] =
    (FileSystem.exists(file) >>= readLines(file) >>= ParseConfigLines.parseLines)
      .catchAll(h =>
        IO.fail(List(ConfigValidation.ErrorReadingFile(file, h.getMessage))))

  private def readLines(file: File)(
      exists: Boolean): RIO[FileSystem, Seq[String]] =
    if (exists) FileSystem.lines(file)
    else ZIO.succeed(Seq.empty)

}

object ParseConfigFile extends ParseConfigFile
