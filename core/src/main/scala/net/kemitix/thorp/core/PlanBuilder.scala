package net.kemitix.thorp.core

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console._
import net.kemitix.thorp.core.Action._
import net.kemitix.thorp.core.hasher.Hasher
import net.kemitix.thorp.domain._
import net.kemitix.thorp.filesystem.FileSystem
import net.kemitix.thorp.storage.api.Storage
import zio.{RIO, ZIO}

object PlanBuilder {

  def createPlan
    : RIO[Storage with Console with Config with FileSystem with Hasher,
          SyncPlan] =
    SyncLogging.logRunStart *> buildPlan

  private def buildPlan =
    gatherMetadata >>= assemblePlan

  private def gatherMetadata =
    fetchRemoteData &&& findLocalFiles

  private def fetchRemoteData =
    for {
      bucket  <- Config.bucket
      prefix  <- Config.prefix
      objects <- Storage.list(bucket, prefix)
    } yield objects

  private def assemblePlan(metadata: (RemoteObjects, LocalFiles)) =
    metadata match {
      case (remoteObjects, localData) =>
        createActions(remoteObjects, localData.localFiles)
          .map(_.filter(doesSomething).sortBy(SequencePlan.order))
          .map(
            SyncPlan(_, SyncTotals(localData.count, localData.totalSizeBytes)))
    }

  private def createActions(
      remoteObjects: RemoteObjects,
      localFiles: Stream[LocalFile]
  ) =
    for {
      fileActions   <- actionsForLocalFiles(remoteObjects, localFiles)
      remoteActions <- actionsForRemoteKeys(remoteObjects.byKey.keys)
    } yield fileActions ++ remoteActions

  private def doesSomething: Action => Boolean = {
    case _: DoNothing => false
    case _            => true
  }

  private def actionsForLocalFiles(
      remoteObjects: RemoteObjects,
      localFiles: Stream[LocalFile]
  ) =
    ZIO.foldLeft(localFiles)(Stream.empty[Action])((acc, localFile) =>
      createActionFromLocalFile(remoteObjects, acc, localFile).map(_ #:: acc))

  private def createActionFromLocalFile(
      remoteObjects: RemoteObjects,
      previousActions: Stream[Action],
      localFile: LocalFile
  ) =
    ActionGenerator.createAction(
      S3MetaDataEnricher.getMetadata(localFile, remoteObjects),
      previousActions)

  private def actionsForRemoteKeys(remoteKeys: Iterable[RemoteKey]) =
    ZIO.foldLeft(remoteKeys)(Stream.empty[Action])((acc, remoteKey) =>
      createActionFromRemoteKey(remoteKey).map(_ #:: acc))

  private def createActionFromRemoteKey(remoteKey: RemoteKey) =
    for {
      bucket       <- Config.bucket
      prefix       <- Config.prefix
      sources      <- Config.sources
      needsDeleted <- Remote.isMissingLocally(sources, prefix)(remoteKey)
    } yield
      if (needsDeleted) ToDelete(bucket, remoteKey, 0L)
      else DoNothing(bucket, remoteKey, 0L)

  private def findLocalFiles =
    SyncLogging.logFileScan *> findFiles

  private def findFiles =
    for {
      sources <- Config.sources
      found   <- ZIO.foreach(sources.paths)(LocalFileStream.findFiles)
    } yield LocalFiles.reduce(found.toStream)

}
