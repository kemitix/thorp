package net.kemitix.thorp.config

import java.io.File
import java.nio.file.Files

import zio.{IO, Task, ZIO}

import scala.jdk.CollectionConverters._

trait ParseConfigFile {

  def parseFile(file: File): ZIO[Any, Seq[ConfigValidation], ConfigOptions] =
    (ZIO(file.exists()) >>= readLines(file) >>= ParseConfigLines.parseLines)
      .catchAll(h =>
        IO.fail(List(ConfigValidation.ErrorReadingFile(file, h.getMessage))))

  private def readLines(file: File)(exists: Boolean): Task[Seq[String]] =
    if (exists) Task(Files.readAllLines(file.toPath).asScala.toSeq)
    else ZIO.succeed(List.empty)

}

object ParseConfigFile extends ParseConfigFile
