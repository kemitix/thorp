package net.kemitix.s3thorp.aws.lib

import cats.Monad
import net.kemitix.s3thorp.domain.Terminal.clearLine
import net.kemitix.s3thorp.domain.SizeTranslation.sizeInEnglish
import net.kemitix.s3thorp.domain.LocalFile

object UploaderLogging {

  def logMultiPartUploadStart[M[_]: Monad](localFile: LocalFile,
                              tryCount: Int)
                             (implicit info: Int => String => M[Unit]): M[Unit] = {
      val tryMessage = if (tryCount == 1) "" else s"try $tryCount"
      val size = sizeInEnglish(localFile.file.length)
      info(1)(s"${clearLine}upload:$tryMessage:$size:${localFile.remoteKey.key}")
    }

  def logMultiPartUploadFinished[M[_]: Monad](localFile: LocalFile)
                                (implicit info: Int => String => M[Unit]): M[Unit] =
    info(4)(s"upload:finished: ${localFile.remoteKey.key}")

}
