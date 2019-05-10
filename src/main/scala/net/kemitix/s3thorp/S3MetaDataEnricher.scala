package net.kemitix.s3thorp

import java.io.File

import fs2.Stream
import cats.effect.IO
import net.kemitix.s3thorp.awssdk.S3Client

trait S3MetaDataEnricher extends S3Client with KeyGenerator {

  def enrichWithS3MetaData(c: Config): File => Stream[IO, Either[File, S3MetaData]] = {
    val remoteKey = generateKey(c)_
    file =>
      Stream.eval({
        val key = remoteKey(file)
        for {
          head <- objectHead(c.bucket, key)
        } yield head.map {
          case (hash, lastModified) => {
            val cleanHash = hash.filter{c=>c!='"'}
            Right(S3MetaData(file, key, cleanHash, lastModified))
          }
        }.getOrElse(Left(file))
      })
  }
}
