package net.kemitix.thorp.filesystem

import java.nio.file.{Path, Paths}
import java.time.Instant
import java.util.regex.Pattern

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import net.kemitix.thorp.domain.{Hashes, LastModified, MD5Hash}
import zio.{UIO, ZIO}

/**
  * Meta data for files in the current source, as of the last time Thorp processed this directory.
  *
  * <p>N.B. Does not include sub-directories.</p>
  */
final case class PathCache(
    data: PathCache.Data
) {
  def get(path: Path): Option[FileData] = data.get(path)
}

object PathCache {
  type Data = Map[Path, FileData]
  val fileName     = ".thorp.cache"
  val tempFileName = ".thorp.cache.tmp"

  def create(path: Path, fileData: FileData): UIO[Iterable[String]] =
    UIO(unsafeCreate(path, fileData))

  def unsafeCreate(path: Path, fileData: FileData): Set[String] = {
    fileData.hashes.keys.asScala
      .map(hashType => {
        val hash     = fileData.hashes.get(hashType).toScala
        val modified = fileData.lastModified
        val hashHash = hash.get.hash
        String.join(":",
                    hashType.label,
                    hashHash,
                    modified.at.toEpochMilli.toString,
                    path.toString)
      })
      .toSet
  }

  private val pattern =
    "^(?<hashtype>.+):(?<hash>.+):(?<modified>\\d+):(?<filename>.+)$"
  private val format = Pattern.compile(pattern)
  def fromLines(lines: Seq[String]): ZIO[Hasher, Nothing, PathCache] =
    ZIO
      .foreach(
        lines
          .map(format.matcher(_))
          .filter(_.matches())) { matcher =>
        for {
          hashType <- Hasher.typeFrom(matcher.group("hashtype"))
        } yield
          Paths.get(matcher.group("filename")) -> FileData
            .create(
              Hashes.create(hashType, MD5Hash.create(matcher.group("hash"))),
              LastModified.at(
                Instant.ofEpochMilli(matcher.group("modified").toLong))
            )
      }
      .catchAll({ _: IllegalArgumentException =>
        UIO(List.empty)
      })
      .map(list => mergeFileData(list))
      .map(map => PathCache(map))

  private def mergeFileData(
      list: List[(Path, FileData)]
  ): Data = {
    list.foldLeft(Map.empty[Path, FileData]) { (acc, pair) =>
      val (fileName, fileData) = pair
      acc.updatedWith(fileName)(
        _.map(fd => fd + fileData)
          .orElse(Some(fileData)))
    }
  }
}
