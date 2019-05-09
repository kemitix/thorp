package net.kemitix.s3thorp

import java.io.File

import fs2.Stream
import cats.effect.IO
import net.kemitix.s3thorp.awssdk.S3Client

trait S3MetaDataEnricher extends S3Client {

  def generateKey(c: Config)(file: File): String = {
    val otherPath = file.toPath.toAbsolutePath
    val sourcePath = c.source.toPath
    val relativePath = sourcePath.relativize(otherPath)
    s"${c.prefix}/$relativePath"
  }

  def enrichWithS3MetaData(c: Config): File => Stream[IO, Either[File, S3MetaData]] = {
    val fileToString = generateKey(c)_
    file =>
      Stream.eval({
        val key = fileToString(file)
        for {
          head <- objectHead(c.bucket, key)
        } yield head.map {
          case (hash,lastModified) =>
            Right(S3MetaData(file, key, hash, lastModified))
        }.getOrElse(Left(file))
      })
  }
}
