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
    } yield
      genAction(formattedMetadata(matchedMetadata, previousActions), bucket)

  private def formattedMetadata(
      matchedMetadata: MatchedMetadata,
      previousActions: Stream[Action]): TaggedMetadata = {
    val remoteExists = matchedMetadata.matchByKey.nonEmpty
    val remoteMatches = remoteExists && matchedMetadata.matchByKey.exists(m =>
      LocalFile.matchesHash(matchedMetadata.localFile)(m.hash))
    val anyMatches = matchedMetadata.matchByHash.nonEmpty
    TaggedMetadata(matchedMetadata,
                   previousActions,
                   remoteExists,
                   remoteMatches,
                   anyMatches)
  }

  case class TaggedMetadata(
      matchedMetadata: MatchedMetadata,
      previousActions: Stream[Action],
      remoteExists: Boolean,
      remoteMatches: Boolean,
      anyMatches: Boolean
  )

  private def genAction(taggedMetadata: TaggedMetadata,
                        bucket: Bucket): Action = {
    taggedMetadata match {
      case TaggedMetadata(md, _, exists, matches, _) if exists && matches =>
        doNothing(bucket, md.localFile.remoteKey)
      case TaggedMetadata(md, _, _, _, any) if any =>
        copyFile(bucket, md.localFile, md.matchByHash.head)
      case TaggedMetadata(md, previous, _, _, _)
          if isUploadAlreadyQueued(previous)(md.localFile) =>
        uploadFile(bucket, md.localFile)
      case TaggedMetadata(md, _, _, _, _) =>
        doNothing(bucket, md.localFile.remoteKey)
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
