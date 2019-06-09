package net.kemitix.s3thorp.aws.lib

import cats.effect.IO
import com.amazonaws.services.s3.model.{AmazonS3Exception, InitiateMultipartUploadResult, UploadPartRequest, UploadPartResult}
import net.kemitix.s3thorp.domain.{LocalFile, MD5Hash}

object S3ClientTransferManagerLogging {

  def logMultiPartUploadStart(localFile: LocalFile,
                              tryCount: Int)
                             (implicit info: Int => String => IO[Unit]): IO[Unit] =
    {
      val tryMessage = if (tryCount == 1) "" else s"try $tryCount"
      val size = sizeInEnglish(localFile)
      info(1)(s"upload:$tryMessage:$size:${localFile.remoteKey.key}")
    }

  private def sizeInEnglish(localFile: LocalFile) =
    localFile.file.length match {
      case bytes if bytes > 1024 * 1024 * 1024 => s"${bytes / 1024 / 1024 /1024}Gb"
      case bytes if bytes > 1024 * 1024 => s"${bytes / 1024 / 1024}Mb"
      case bytes if bytes > 1024 => s"${bytes / 1024}Kb"
      case bytes => s"${localFile.file.length}b"
    }

  def logMultiPartUploadFinished(localFile: LocalFile)
                                (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(4)(s"upload:finished: ${localFile.remoteKey.key}")

}
