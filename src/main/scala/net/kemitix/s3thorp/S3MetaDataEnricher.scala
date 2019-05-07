package net.kemitix.s3thorp

import java.io.File
import java.time.Instant

import fs2.Stream
import cats.effect.IO
import Main.putStrLn

trait S3MetaDataEnricher extends S3Client {

  def enrichWithS3MetaData: File => Stream[IO, S3MetaData] =
    file => Stream.eval(for {
      _ <- putStrLn(s"enrich: $file")
      // HEAD(bucket, prefix, relative(file))
      // create blank S3MetaData records (sealed trait?)
    } yield S3MetaData(file, "", "", Instant.now()))

}
