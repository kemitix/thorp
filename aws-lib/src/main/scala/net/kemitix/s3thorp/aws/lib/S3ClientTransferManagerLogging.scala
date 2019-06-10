package net.kemitix.s3thorp.aws.lib

import cats.effect.IO
import net.kemitix.s3thorp.domain.SizeTranslation.sizeInEnglish
import net.kemitix.s3thorp.domain.LocalFile

object S3ClientTransferManagerLogging {

  def logMultiPartUploadStart(localFile: LocalFile,
                              tryCount: Int)
                             (implicit info: Int => String => IO[Unit]): IO[Unit] =
    {
      val tryMessage = if (tryCount == 1) "" else s"try $tryCount"
      val size = sizeInEnglish(localFile.file.length)
      info(1)(s"upload:$tryMessage:$size:${localFile.remoteKey.key}")
    }

  def logMultiPartUploadFinished(localFile: LocalFile)
                                (implicit info: Int => String => IO[Unit]): IO[Unit] =
    info(4)(s"upload:finished: ${localFile.remoteKey.key}")

}
