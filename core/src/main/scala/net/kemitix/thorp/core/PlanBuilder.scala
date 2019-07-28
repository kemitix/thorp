package net.kemitix.thorp.core

import net.kemitix.thorp.console._
import net.kemitix.thorp.core.Action._
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage._
import net.kemitix.thorp.storage.api.{HashService, Storage}
import zio.{Task, TaskR}

trait PlanBuilder {

  def createPlan(
      hashService: HashService,
      configOptions: ConfigOptions
  ): TaskR[Storage with Console, SyncPlan] =
    ConfigurationBuilder
      .buildConfig(configOptions)
      .catchAll(errors => TaskR.fail(ConfigValidationException(errors)))
      .flatMap(config => useValidConfig(hashService)(config))

  private def useValidConfig(
      hashService: HashService
  )(implicit c: Config) = {
    for {
      _       <- SyncLogging.logRunStart(c.bucket, c.prefix, c.sources)
      actions <- buildPlan(hashService)
    } yield actions
  }

  private def buildPlan(
      hashService: HashService
  )(implicit c: Config) =
    for {
      metadata <- gatherMetadata(hashService)
    } yield assemblePlan(c)(metadata)

  private def assemblePlan(
      implicit c: Config): ((S3ObjectsData, LocalFiles)) => SyncPlan = {
    case (remoteData, localData) =>
      SyncPlan(
        actions = createActions(c)(remoteData)(localData)
          .filter(doesSomething)
          .sortBy(SequencePlan.order),
        syncTotals = SyncTotals(count = localData.count,
                                totalSizeBytes = localData.totalSizeBytes)
      )
  }

  private def createActions
    : Config => S3ObjectsData => LocalFiles => Stream[Action] =
    c =>
      remoteData =>
        localData =>
          actionsForLocalFiles(c)(remoteData)(localData) ++
            actionsForRemoteKeys(c)(remoteData)

  private def doesSomething: Action => Boolean = {
    case _: DoNothing => false
    case _            => true
  }

  private def actionsForLocalFiles
    : Config => S3ObjectsData => LocalFiles => Stream[Action] =
    c =>
      remoteData =>
        localData =>
          localData.localFiles.foldLeft(Stream.empty[Action])((acc, lf) =>
            createActionFromLocalFile(c)(lf)(remoteData)(acc) ++ acc)

  private def createActionFromLocalFile
    : Config => LocalFile => S3ObjectsData => Stream[Action] => Stream[Action] =
    c =>
      lf =>
        remoteData =>
          previousActions =>
            ActionGenerator.createActions(
              S3MetaDataEnricher.getMetadata(lf, remoteData)(c),
              previousActions)(c)

  private def actionsForRemoteKeys: Config => S3ObjectsData => Stream[Action] =
    c =>
      remoteData =>
        remoteData.byKey.keys.foldLeft(Stream.empty[Action])((acc, rk) =>
          createActionFromRemoteKey(c)(rk) #:: acc)

  private def createActionFromRemoteKey: Config => RemoteKey => Action =
    c =>
      rk =>
        if (rk.isMissingLocally(c.sources, c.prefix))
          Action.ToDelete(c.bucket, rk, 0L)
        else DoNothing(c.bucket, rk, 0L)

  private def gatherMetadata(
      hashService: HashService
  )(implicit c: Config) =
    for {
      remoteData <- fetchRemoteData
      localData  <- findLocalFiles(hashService)
    } yield (remoteData, localData)

  private def fetchRemoteData(implicit c: Config) =
    listObjects(c.bucket, c.prefix)

  private def findLocalFiles(
      hashService: HashService
  )(implicit config: Config) =
    for {
      _          <- SyncLogging.logFileScan
      localFiles <- findFiles(hashService)
    } yield localFiles

  private def findFiles(
      hashService: HashService
  )(implicit c: Config) = {
    Task
      .foreach(c.sources.paths)(LocalFileStream.findFiles(_, hashService))
      .map(_.foldLeft(LocalFiles())((acc, localFile) => acc ++ localFile))
  }

}

object PlanBuilder extends PlanBuilder
