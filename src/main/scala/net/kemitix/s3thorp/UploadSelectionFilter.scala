package net.kemitix.s3thorp

import java.io.File

import fs2.Stream
import cats.effect.IO
import net.kemitix.s3thorp.Main.putStrLn

trait UploadSelectionFilter {

  def uploadRequiredFilter: Either[File, S3MetaData] => Stream[IO, File] = {
    case Left(file) => Stream(file)
    case Right(s3Metadata) =>
      Stream.eval(for {
        _ <- putStrLn(s"upload required: ${s3Metadata.localFile}")
        //md5File(localFile)
        //filter(localHash => options.force || localHash != metadataHash)
      } yield s3Metadata.localFile)
  }

}
