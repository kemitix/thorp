package net.kemitix.s3thorp

import java.io.File

trait ActionGenerator
  extends MD5HashGenerator {

  def createActions(s3MetaData: S3MetaData)
                   (implicit c: Config): Stream[Action] =
    s3MetaData match {

      // There is a local file, but nothing matching in S3 - Upload
      case S3MetaData(localFile, None) => uploadFile(localFile)

      // There is a local file and an s3 file with the same name, but different content - Upload
      case S3MetaData(localFile, Some((_, remoteHash, _)))
        if md5File(localFile) != remoteHash => uploadFile(localFile)

      case _ => Stream.empty
    }

  private def uploadFile(localFile: File)= {
    Stream(ToUpload(localFile))
  }
}
