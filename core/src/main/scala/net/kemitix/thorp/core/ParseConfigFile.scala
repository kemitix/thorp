package net.kemitix.thorp.core

import java.nio.file.{Files, Path, Paths}
import java.util.regex.Pattern

import cats.effect.IO
import net.kemitix.thorp.core.ConfigOption._

import scala.collection.JavaConverters._

trait ParseConfigFile {

  def apply(filename: Path): IO[Seq[ConfigOption]] =
    readFile(filename).map(parseLines)

  private def readFile(filename: Path) =
    IO {
      val lines = Files.lines(filename)
      val scala = lines.iterator.asScala.toList
      lines.close()
      scala
    }

  private def parseLines(lines: List[String]) =
    lines.flatMap(parseLine)

  private val format = Pattern.compile("^(<?:key>)\\s*=\\s*(<?:value>)\\s*$")

  private def parseLine(str: String) =
    format.matcher(str) match {
      case m if m.matches => parse(m.group("key"), m.group("value"))
      case _ => None
    }

  private def parse(key: String, value: String): Option[ConfigOption] =
    key match {
      case "source" => Some(Source(Paths.get(value)))
      case "bucket" => Some(Bucket(value))
      case "prefix" => Some(Prefix(value))
      case "include" => Some(Include(value))
      case "exclude" => Some(Exclude(value))
      case "debug" => Some(Debug())
      case _ => None
    }

}

object ParseConfigFile extends ParseConfigFile