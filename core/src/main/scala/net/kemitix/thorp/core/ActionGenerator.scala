package net.kemitix.thorp.core

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToUpload}
import net.kemitix.thorp.domain._
import zio.ZIO

object ActionGenerator {

  def createActions(
      s3MetaData: S3MetaData,
      previousActions: Stream[Action]
  ): ZIO[Config, Nothing, Stream[Action]] =
    for {
      bucket <- Config.bucket
    } yield
      s3MetaData match {
        // #1 local exists, remote exists, remote matches - do nothing
        case S3MetaData(localFile, _, Some(RemoteMetaData(key, hash, _)))
            if localFile.matches(hash) =>
          doNothing(bucket, key)
        // #2 local exists, remote is missing, other matches - copy
        case S3MetaData(localFile, matchByHash, None) if matchByHash.nonEmpty =>
          copyFile(bucket, localFile, matchByHash)
        // #3 local exists, remote is missing, other no matches - upload
        case S3MetaData(localFile, matchByHash, None)
            if matchByHash.isEmpty &&
              isUploadAlreadyQueued(previousActions)(localFile) =>
          uploadFile(bucket, localFile)
        // #4 local exists, remote exists, remote no match, other matches - copy
        case S3MetaData(localFile,
                        matchByHash,
                        Some(RemoteMetaData(_, hash, _)))
            if !localFile.matches(hash) &&
              matchByHash.nonEmpty =>
          copyFile(bucket, localFile, matchByHash)
        // #5 local exists, remote exists, remote no match, other no matches - upload
        case S3MetaData(localFile, matchByHash, Some(_))
            if matchByHash.isEmpty =>
          uploadFile(bucket, localFile)
        // fallback
        case S3MetaData(localFile, _, _) =>
          doNothing(bucket, localFile.remoteKey)
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
  ) =
    Stream(DoNothing(bucket, remoteKey, 0L))

  private def uploadFile(
      bucket: Bucket,
      localFile: LocalFile
  ) =
    Stream(ToUpload(bucket, localFile, localFile.file.length))

  private def copyFile(
      bucket: Bucket,
      localFile: LocalFile,
      matchByHash: Set[RemoteMetaData]
  ): Stream[Action] =
    matchByHash
      .map { remoteMetaData =>
        ToCopy(bucket,
               remoteMetaData.remoteKey,
               remoteMetaData.hash,
               localFile.remoteKey,
               localFile.file.length)
      }
      .toStream
      .take(1)

}
