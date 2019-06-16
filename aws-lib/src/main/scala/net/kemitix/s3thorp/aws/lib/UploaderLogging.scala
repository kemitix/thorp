package net.kemitix.s3thorp.aws.lib

import cats.Monad
import net.kemitix.thorp.domain.SizeTranslation.sizeInEnglish
import net.kemitix.thorp.domain.Terminal.clearLine
import net.kemitix.thorp.domain.{LocalFile, Logger}

object UploaderLogging {

  def logMultiPartUploadStart[M[_]: Monad](localFile: LocalFile,
                              tryCount: Int)
                             (implicit logger: Logger[M]): M[Unit] = {
      val tryMessage = if (tryCount == 1) "" else s"try $tryCount"
      val size = sizeInEnglish(localFile.file.length)
      logger.info(s"${clearLine}upload:$tryMessage:$size:${localFile.remoteKey.key}")
    }

  def logMultiPartUploadFinished[M[_]: Monad](localFile: LocalFile)
                                (implicit logger: Logger[M]): M[Unit] =
    logger.debug(s"upload:finished: ${localFile.remoteKey.key}")

}
