package net.kemitix.thorp.core

import java.nio.file.Paths
import java.util.regex.Pattern

import net.kemitix.thorp.core.ConfigOption._

trait ParseConfigLines {

  def parseLines(lines: List[String]): ConfigOptions =
    ConfigOptions(lines.flatMap(parseLine))

  private val pattern = "^\\s*(?<key>\\S*)\\s*=\\s*(?<value>\\S*)\\s*$"
  private val format = Pattern.compile(pattern)

  private def parseLine(str: String) =
    format.matcher(str) match {
      case m if m.matches => parseKeyValue(m.group("key"), m.group("value"))
      case _ =>None
    }

  def truthy(value: String): Boolean =
    value.toLowerCase match {
      case "true" => true
      case "yes" => true
      case "enabled" => true
      case _ => false
    }

  private def parseKeyValue(key: String, value: String): Option[ConfigOption] =
    key.toLowerCase match {
      case "source" => Some(Source(Paths.get(value)))
      case "bucket" => Some(Bucket(value))
      case "prefix" => Some(Prefix(value))
      case "include" => Some(Include(value))
      case "exclude" => Some(Exclude(value))
      case "debug" => if (truthy(value)) Some(Debug()) else None
      case _ => None
    }

}

object ParseConfigLines extends ParseConfigLines
