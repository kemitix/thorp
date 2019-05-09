package net.kemitix.s3thorp

import java.io.File

import fs2.Stream
import cats.effect.IO
import net.kemitix.s3thorp.Main.putStrLn
import net.kemitix.s3thorp.awssdk.S3Client

import scala.concurrent.Promise

trait S3Uploader extends S3Client {

  def performUpload: File => Stream[IO, Promise[Unit]] =
    file => Stream.eval(for {
      _ <- putStrLn(s"uploading: $file")
      // upload
      p = Promise[Unit]()
    } yield p)

}
