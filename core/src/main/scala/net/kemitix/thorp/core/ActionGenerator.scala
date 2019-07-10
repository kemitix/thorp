package net.kemitix.thorp.core

import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToUpload}
import net.kemitix.thorp.domain._

object ActionGenerator {

  def remoteNameNotAlreadyQueued(localFile: LocalFile,
                                 previousActions: Stream[Action]): Boolean = {
    val key = localFile.remoteKey.key
    !previousActions.exists {
      case ToUpload(_, lf, _) => lf.remoteKey.key equals key
      case _ => false
    }
  }

  def createActions(s3MetaData: S3MetaData,
                    previousActions: Stream[Action])
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
        if otherMatches.isEmpty &&
          remoteNameNotAlreadyQueued(localFile, previousActions)
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

      case S3MetaData(localFile, _, _) =>
        doNothing(c.bucket, localFile.remoteKey)
    }

  private def doNothing(bucket: Bucket,
                        remoteKey: RemoteKey) =
    Stream(
      DoNothing(bucket, remoteKey, 0L))

  private def uploadFile(bucket: Bucket,
                         localFile: LocalFile) =
    Stream(
      ToUpload(bucket, localFile, localFile.file.length))

  private def copyFile(bucket: Bucket,
                       localFile: LocalFile,
                       matchByHash: Set[RemoteMetaData]): Stream[Action] = {
    val headOption = matchByHash.headOption
    headOption.toStream.map { remoteMetaData =>
      val sourceKey = remoteMetaData.remoteKey
      val hash = remoteMetaData.hash
      ToCopy(bucket, sourceKey, hash, localFile.remoteKey, localFile.file.length)
    }
  }

}
