package net.kemitix.thorp.filesystem

import java.time.Instant
import java.util.regex.Pattern

import net.kemitix.thorp.domain.{HashType, MD5Hash}
import zio.{UIO, ZIO}

/**
  * Meta data for files in the current directory, as of the last time Thorp processed this directory.
  *
  * <p>N.B. Does not include sub-directories.</p>
  */
final case class PathCache(
    data: Map[FileName, FileData]
) {}

object PathCache {
  private val pattern =
    "^(?<hashtype>.+):(?<hash>.+):(?<modified>.+):(?<filename>.+)$"
  private val format = Pattern.compile(pattern)
  def fromLines(lines: Seq[String]): UIO[PathCache] = {
    ZIO
      .foreach(
        lines
          .map(format.matcher(_))
          .filter(_.matches())) { matcher =>
        for {
          hashType <- HashType.from(matcher.group("hashType"))
        } yield
          (matcher.group("filename") -> FileData
            .create(
              hashType,
              MD5Hash(matcher.group("hash")),
              Instant.parse(matcher.group("modified"))
            ))
      }
      .map(list => Map.from(list))
      .map(map => PathCache(map))
  }
}
