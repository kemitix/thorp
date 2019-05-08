package net.kemitix.s3thorp

import java.io.File

import fs2.Stream
import cats.effect.IO
import Main.putStrLn

trait S3MetaDataEnricher extends S3Client {

  def generateKey(c: Config)(file: File): String = {
    s"${c.prefix}/${c.source.toPath.relativize(file.toPath)}"
  }

  def enrichWithS3MetaData(c: Config): File => Stream[IO, S3MetaData] = {
    val fileToString = generateKey(c)_
    file =>
      Stream.eval(for {
        _ <- putStrLn(s"enrich: $file")
        key = fileToString(file)
        head <- objectHead(c.bucket, key)
        (hash, lastModified) = head
      } yield S3MetaData(file, key, hash, lastModified))
  }
}
