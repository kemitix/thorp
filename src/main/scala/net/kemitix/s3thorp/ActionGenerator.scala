package net.kemitix.s3thorp


trait ActionGenerator
  extends MD5HashGenerator {

  def createActions(s3MetaData: S3MetaData)
                   (implicit c: Config): Stream[Action] =
    s3MetaData match {

      // There is a local file, but nothing matching in S3 - Upload
      case S3MetaData(localFile, hashMatches, None) if hashMatches.isEmpty => uploadFile(localFile)

      // There is a local file and an s3 file with the same name, but different content,
      // and no other object with the same content - Upload
      case S3MetaData(localFile, hashMatches, Some(RemoteMetaData(_, remoteHash, _)))
        if hashMatches.isEmpty && localFile.hash != remoteHash => uploadFile(localFile)

      case _ => Stream.empty
    }

  private def uploadFile(localFile: LocalFile) = Stream(ToUpload(localFile))
}
