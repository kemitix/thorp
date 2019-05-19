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

      // #2 local exists, remote is missing, other matches - copy
      case S3MetaData(localFile, matchByHash, None)
        if matchByHash.nonEmpty => copyFile(localFile, matchByHash)

      case _ => Stream.empty
    }

  private def uploadFile(localFile: LocalFile) = Stream(ToUpload(localFile))

  private def copyFile(localFile: LocalFile, matchByHash: Set[RemoteMetaData]): Stream[Action] =
    Stream(ToCopy(
      sourceKey = matchByHash.head.remoteKey,
      hash = localFile.hash,
      targetKey = localFile.remoteKey
    ))

}
