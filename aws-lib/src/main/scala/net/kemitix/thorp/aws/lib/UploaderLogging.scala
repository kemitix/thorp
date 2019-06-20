package net.kemitix.thorp.aws.lib

import cats.effect.IO
import net.kemitix.thorp.domain.SizeTranslation.sizeInEnglish
import net.kemitix.thorp.domain.Terminal.clearLine
import net.kemitix.thorp.domain.{LocalFile, Logger}

object UploaderLogging {

  def logMultiPartUploadStart(localFile: LocalFile,
                              tryCount: Int)
                             (implicit logger: Logger): IO[Unit] = {
      val tryMessage = if (tryCount == 1) "" else s"try $tryCount"
      val size = sizeInEnglish(localFile.file.length)
      logger.info(s"${clearLine}upload:$tryMessage:$size:${localFile.remoteKey.key}")
    }

  def logMultiPartUploadFinished(localFile: LocalFile)
                                (implicit logger: Logger): IO[Unit] =
    logger.debug(s"upload:finished: ${localFile.remoteKey.key}")

}
