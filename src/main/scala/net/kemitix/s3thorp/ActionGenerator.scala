package net.kemitix.s3thorp

trait ActionGenerator
  extends MD5HashGenerator {

  def createActions(s3MetaData: S3MetaData)
                   (implicit c: Config): Stream[Action] =
    s3MetaData match {

      // #1 local exists, remote exists, remote matches - do nothing
      case S3MetaData(localFile, _, Some(RemoteMetaData(_, remoteHash, _)))
        if localFile.hash == remoteHash
      => Stream.empty

      // #2 local exists, remote is missing, other matches - copy
      case S3MetaData(localFile, otherMatches, None)
        if otherMatches.nonEmpty
      => copyFile(localFile, otherMatches)

      // #3 local exists, remote is missing, other no matches - upload
      case S3MetaData(localFile, otherMatches, None)
        if otherMatches.isEmpty
      => uploadFile(localFile)

      // #4 local exists, remote exists, remote no match, other matches - copy
      case S3MetaData(localFile, otherMatches, Some(RemoteMetaData(_, remoteHash, _)))
        if localFile.hash != remoteHash &&
          otherMatches.nonEmpty
      => copyFile(localFile, otherMatches)

      // #5 local exists, remote exists, remote no match, other no matches - upload
      case S3MetaData(localFile, hashMatches, Some(_))
        if hashMatches.isEmpty
      => uploadFile(localFile)

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
