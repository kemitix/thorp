package net.kemitix.thorp.core

import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToUpload}
import net.kemitix.thorp.domain._

object ActionGenerator {

  def createActions(s3MetaData: S3MetaData)
                   (implicit c: Config): Stream[Action] =
    s3MetaData match {

      // #1 local exists, remote exists, remote matches - do nothing
      case S3MetaData(localFile, _, Some(RemoteMetaData(remoteKey, remoteHash, _)))
        if localFile.matches(remoteHash)
      => doNothing(c.bucket, remoteKey)

      // #2 local exists, remote is missing, other matches - copy
      case S3MetaData(localFile, otherMatches, None)
        if otherMatches.nonEmpty
      => copyFile(c.bucket, localFile, otherMatches)

      // #3 local exists, remote is missing, other no matches - upload
      case S3MetaData(localFile, otherMatches, None)
        if otherMatches.isEmpty
      => uploadFile(c.bucket, localFile)

      // #4 local exists, remote exists, remote no match, other matches - copy
      case S3MetaData(localFile, otherMatches, Some(RemoteMetaData(_, remoteHash, _)))
        if !localFile.matches(remoteHash) &&
          otherMatches.nonEmpty
      => copyFile(c.bucket, localFile, otherMatches)

      // #5 local exists, remote exists, remote no match, other no matches - upload
      case S3MetaData(localFile, hashMatches, Some(_))
        if hashMatches.isEmpty
      => uploadFile(c.bucket, localFile)

    }

  private def doNothing(bucket: Bucket,
                        remoteKey: RemoteKey) =
    Stream(
      DoNothing(bucket, remoteKey))

  private def uploadFile(bucket: Bucket,
                         localFile: LocalFile) =
    Stream(
      ToUpload(bucket, localFile))

  private def copyFile(bucket: Bucket,
                       localFile: LocalFile,
                       matchByHash: Set[RemoteMetaData]): Stream[Action] =
    matchByHash.headOption.map(_.remoteKey).toStream.map {sourceKey =>
      ToCopy(bucket, sourceKey, localFile.hash, localFile.remoteKey)}

}
