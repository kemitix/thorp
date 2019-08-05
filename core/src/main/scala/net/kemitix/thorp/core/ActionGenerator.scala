package net.kemitix.thorp.core

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToUpload}
import net.kemitix.thorp.domain._
import zio.RIO

object ActionGenerator {

  def createAction(
      matchedMetadata: MatchedMetadata,
      previousActions: Stream[Action]
  ): RIO[Config, Action] =
    for {
      bucket <- Config.bucket
    } yield genAction(matchedMetadata, previousActions, bucket)

  private def genAction(matchedMetadata: MatchedMetadata,
                        previousActions: Stream[Action],
                        bucket: Bucket): Action = {
    matchedMetadata match {
      // #1 local exists, remote exists, remote matches - do nothing
      case MatchedMetadata(localFile, _, Some(RemoteMetaData(key, hash, _)))
          if LocalFile.matchesHash(localFile)(hash) =>
        doNothing(bucket, key)
      // #2 local exists, remote is missing, other matches - copy
      case MatchedMetadata(localFile, matchByHash, None)
          if matchByHash.nonEmpty =>
        copyFile(bucket, localFile, matchByHash.head)
      // #3 local exists, remote is missing, other no matches - upload
      case MatchedMetadata(localFile, matchByHash, None)
          if matchByHash.isEmpty &&
            isUploadAlreadyQueued(previousActions)(localFile) =>
        uploadFile(bucket, localFile)
      // #4 local exists, remote exists, remote no match, other matches - copy
      case MatchedMetadata(localFile,
                           matchByHash,
                           Some(RemoteMetaData(_, hash, _)))
          if !LocalFile.matchesHash(localFile)(hash) &&
            matchByHash.nonEmpty =>
        copyFile(bucket, localFile, matchByHash.head)
      // #5 local exists, remote exists, remote no match, other no matches - upload
      case MatchedMetadata(localFile, matchByHash, Some(_))
          if matchByHash.isEmpty =>
        uploadFile(bucket, localFile)
      // fallback
      case MatchedMetadata(localFile, _, _) =>
        doNothing(bucket, localFile.remoteKey)
    }
  }

  private def key = LocalFile.remoteKey ^|-> RemoteKey.key

  def isUploadAlreadyQueued(
      previousActions: Stream[Action]
  )(
      localFile: LocalFile
  ): Boolean = !previousActions.exists {
    case ToUpload(_, lf, _) => key.get(lf) equals key.get(localFile)
    case _                  => false
  }

  private def doNothing(
      bucket: Bucket,
      remoteKey: RemoteKey
  ) = DoNothing(bucket, remoteKey, 0L)

  private def uploadFile(
      bucket: Bucket,
      localFile: LocalFile
  ) = ToUpload(bucket, localFile, localFile.file.length)

  private def copyFile(
      bucket: Bucket,
      localFile: LocalFile,
      remoteMetaData: RemoteMetaData
  ): Action =
    ToCopy(bucket,
           remoteMetaData.remoteKey,
           remoteMetaData.hash,
           localFile.remoteKey,
           localFile.file.length)

}
