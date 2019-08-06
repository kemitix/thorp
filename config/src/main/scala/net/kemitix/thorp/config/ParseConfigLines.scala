package net.kemitix.thorp.config

import java.nio.file.Paths
import java.util.regex.Pattern

import net.kemitix.thorp.config.ConfigOption._
import zio.UIO

trait ParseConfigLines {

  private val pattern = "^\\s*(?<key>\\S*)\\s*=\\s*(?<value>\\S*)\\s*$"
  private val format  = Pattern.compile(pattern)

  def parseLines(lines: Seq[String]): UIO[ConfigOptions] =
    UIO(ConfigOptions(lines.flatMap(parseLine).toList))

  private def parseLine(str: String) =
    format.matcher(str) match {
      case m if m.matches => parseKeyValue(m.group("key"), m.group("value"))
      case _              => List.empty
    }

  private def parseKeyValue(
      key: String,
      value: String
  ): List[ConfigOption] =
    key.toLowerCase match {
      case "source"  => List(Source(Paths.get(value)))
      case "bucket"  => List(Bucket(value))
      case "prefix"  => List(Prefix(value))
      case "include" => List(Include(value))
      case "exclude" => List(Exclude(value))
      case "debug"   => if (truthy(value)) List(Debug()) else List.empty
      case _         => List.empty
    }

  private def truthy(value: String): Boolean =
    value.toLowerCase match {
      case "true"    => true
      case "yes"     => true
      case "enabled" => true
      case _         => false
    }

}

object ParseConfigLines extends ParseConfigLines
