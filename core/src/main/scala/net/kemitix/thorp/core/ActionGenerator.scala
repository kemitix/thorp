package net.kemitix.thorp.core

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToUpload}
import net.kemitix.thorp.domain._
import zio.RIO
import Implicits._

object ActionGenerator {

  def createActions(
      matchedMetadata: MatchedMetadata,
      previousActions: Stream[Action]
  ): RIO[Config, Stream[Action]] =
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

  final case class TaggedMetadata(
      matchedMetadata: MatchedMetadata,
      previousActions: Stream[Action],
      remoteExists: Boolean,
      remoteMatches: Boolean,
      anyMatches: Boolean
  )

  private def genAction(taggedMetadata: TaggedMetadata,
                        bucket: Bucket): Stream[Action] = {
    taggedMetadata match {
      case TaggedMetadata(md, _, remoteExists, remoteMatches, _)
          if remoteExists && remoteMatches =>
        doNothing(bucket, md.localFile.remoteKey)
      case TaggedMetadata(md, _, _, _, anyMatches) if anyMatches =>
        copyFile(bucket, md.localFile, md.matchByHash)
      case TaggedMetadata(md, previous, _, _, _)
          if isNotUploadAlreadyQueued(previous)(md.localFile) =>
        uploadFile(bucket, md.localFile)
      case TaggedMetadata(md, _, _, _, _) =>
        doNothing(bucket, md.localFile.remoteKey)
    }
  }

  private def key = LocalFile.remoteKey ^|-> RemoteKey.key

  def isNotUploadAlreadyQueued(
      previousActions: Stream[Action]
  )(
      localFile: LocalFile
  ): Boolean = !previousActions.exists {
    case ToUpload(_, lf, _) => key.get(lf) === key.get(localFile)
    case _                  => false
  }

  private def doNothing(
      bucket: Bucket,
      remoteKey: RemoteKey
  ) = Stream(DoNothing(bucket, remoteKey, 0L))

  private def uploadFile(
      bucket: Bucket,
      localFile: LocalFile
  ) = Stream(ToUpload(bucket, localFile, localFile.file.length))

  private def copyFile(
      bucket: Bucket,
      localFile: LocalFile,
      remoteMetaData: Set[RemoteMetaData]
  ) =
    remoteMetaData
      .take(1)
      .toStream
      .map(
        other =>
          ToCopy(bucket,
                 other.remoteKey,
                 other.hash,
                 localFile.remoteKey,
                 localFile.file.length))

}
