package net.kemitix.thorp.core

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console._
import net.kemitix.thorp.core.Action._
import net.kemitix.thorp.domain._
import net.kemitix.thorp.filesystem.FileSystem
import net.kemitix.thorp.storage.api.Storage
import zio.{TaskR, ZIO}

trait PlanBuilder {

  def createPlan(hashService: HashService)
    : TaskR[Storage with Console with Config with FileSystem, SyncPlan] =
    for {
      _       <- SyncLogging.logRunStart
      actions <- buildPlan(hashService)
    } yield actions

  private def buildPlan(hashService: HashService) =
    for {
      metadata <- gatherMetadata(hashService)
      plan     <- assemblePlan(metadata)
    } yield plan

  private def assemblePlan(metadata: (S3ObjectsData, LocalFiles)) =
    metadata match {
      case (remoteData, localData) =>
        createActions(remoteData, localData)
          .map(_.filter(doesSomething).sortBy(SequencePlan.order))
          .map(
            SyncPlan(_, SyncTotals(localData.count, localData.totalSizeBytes)))
    }

  private def createActions(
      remoteData: S3ObjectsData,
      localData: LocalFiles
  ) =
    for {
      fileActions   <- actionsForLocalFiles(remoteData, localData)
      remoteActions <- actionsForRemoteKeys(remoteData)
    } yield fileActions ++ remoteActions

  private def doesSomething: Action => Boolean = {
    case _: DoNothing => false
    case _            => true
  }

  private def actionsForLocalFiles(
      remoteData: S3ObjectsData,
      localData: LocalFiles
  ) =
    ZIO.foldLeft(localData.localFiles)(Stream.empty[Action])(
      (acc, localFile) =>
        createActionFromLocalFile(remoteData, acc, localFile)
          .map(actions => actions ++ acc))

  private def createActionFromLocalFile(
      remoteData: S3ObjectsData,
      previousActions: Stream[Action],
      localFile: LocalFile
  ) =
    ActionGenerator.createActions(
      S3MetaDataEnricher.getMetadata(localFile, remoteData),
      previousActions)

  private def actionsForRemoteKeys(remoteData: S3ObjectsData) =
    ZIO.foldLeft(remoteData.byKey.keys)(Stream.empty[Action]) {
      (acc, remoteKey) =>
        createActionFromRemoteKey(remoteKey).map(action => action #:: acc)
    }

  private def createActionFromRemoteKey(remoteKey: RemoteKey) =
    for {
      bucket       <- Config.bucket
      prefix       <- Config.prefix
      sources      <- Config.sources
      needsDeleted <- Remote.isMissingLocally(sources, prefix)(remoteKey)
    } yield
      if (needsDeleted) ToDelete(bucket, remoteKey, 0L)
      else DoNothing(bucket, remoteKey, 0L)

  private def gatherMetadata(hashService: HashService) =
    for {
      remoteData <- fetchRemoteData
      localData  <- findLocalFiles(hashService)
    } yield (remoteData, localData)

  private def fetchRemoteData =
    for {
      bucket  <- Config.bucket
      prefix  <- Config.prefix
      objects <- Storage.list(bucket, prefix)
    } yield objects

  private def findLocalFiles(hashService: HashService) =
    for {
      _          <- SyncLogging.logFileScan
      localFiles <- findFiles(hashService)
    } yield localFiles

  private def findFiles(hashService: HashService) =
    for {
      sources <- Config.sources
      paths = sources.paths
      found <- ZIO.foreach(paths)(path =>
        LocalFileStream.findFiles(hashService)(path))
    } yield LocalFiles.reduce(found.toStream)

}

object PlanBuilder extends PlanBuilder
