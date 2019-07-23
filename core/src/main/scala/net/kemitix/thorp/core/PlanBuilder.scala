package net.kemitix.thorp.core

import net.kemitix.thorp.console._
import net.kemitix.thorp.core.Action._
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.{HashService, StorageService}
import zio.{Task, TaskR}

trait PlanBuilder {

  def createPlan(
      storageService: StorageService,
      hashService: HashService,
      configOptions: ConfigOptions
  ): TaskR[MyConsole, SyncPlan] =
    ConfigurationBuilder
      .buildConfig(configOptions)
      .catchAll(errors => TaskR.fail(ConfigValidationException(errors)))
      .flatMap(config => useValidConfig(storageService, hashService)(config))

  def useValidConfig(
      storageService: StorageService,
      hashService: HashService
  )(implicit c: Config): TaskR[MyConsole, SyncPlan] = {
    for {
      _       <- SyncLogging.logRunStart(c.bucket, c.prefix, c.sources)
      actions <- buildPlan(storageService, hashService)
    } yield actions
  }

  private def buildPlan(
      storageService: StorageService,
      hashService: HashService
  )(implicit c: Config): TaskR[MyConsole, SyncPlan] =
    for {
      metadata <- gatherMetadata(storageService, hashService)
    } yield assemblePlan(c)(metadata)

  def assemblePlan(
      implicit c: Config): ((S3ObjectsData, LocalFiles)) => SyncPlan = {
    case (remoteData, localData) =>
      SyncPlan(
        actions = createActions(remoteData, localData)
          .filter(doesSomething)
          .sortBy(SequencePlan.order),
        syncTotals = SyncTotals(count = localData.count,
                                totalSizeBytes = localData.totalSizeBytes)
      )
  }

  private def createActions(
      remoteData: S3ObjectsData,
      localData: LocalFiles
  )(implicit c: Config): Stream[Action] =
    actionsForLocalFiles(localData, remoteData) ++
      actionsForRemoteKeys(remoteData)

  def doesSomething: Action => Boolean = {
    case _: DoNothing => false
    case _            => true
  }

  private val emptyActionStream = Stream[Action]()

  private def actionsForLocalFiles(
      localData: LocalFiles,
      remoteData: S3ObjectsData
  )(implicit c: Config) =
    localData.localFiles.foldLeft(emptyActionStream)((acc, lf) =>
      createActionFromLocalFile(lf, remoteData, acc) ++ acc)

  private def createActionFromLocalFile(
      lf: LocalFile,
      remoteData: S3ObjectsData,
      previousActions: Stream[Action]
  )(implicit c: Config) =
    ActionGenerator.createActions(
      S3MetaDataEnricher.getMetadata(lf, remoteData),
      previousActions)

  private def actionsForRemoteKeys(remoteData: S3ObjectsData)(
      implicit c: Config) =
    remoteData.byKey.keys.foldLeft(emptyActionStream)((acc, rk) =>
      createActionFromRemoteKey(rk) #:: acc)

  private def createActionFromRemoteKey(rk: RemoteKey)(implicit c: Config) =
    if (rk.isMissingLocally(c.sources, c.prefix))
      Action.ToDelete(c.bucket, rk, 0L)
    else DoNothing(c.bucket, rk, 0L)

  private def gatherMetadata(
      storageService: StorageService,
      hashService: HashService
  )(implicit c: Config): TaskR[MyConsole, (S3ObjectsData, LocalFiles)] =
    for {
      remoteData <- fetchRemoteData(storageService)
      localData  <- findLocalFiles(hashService)
    } yield (remoteData, localData)

  private def fetchRemoteData(
      storageService: StorageService
  )(implicit c: Config): TaskR[MyConsole, S3ObjectsData] =
    storageService.listObjects(c.bucket, c.prefix)

  private def findLocalFiles(
      hashService: HashService
  )(implicit config: Config): TaskR[MyConsole, LocalFiles] =
    for {
      _          <- SyncLogging.logFileScan
      localFiles <- findFiles(hashService)
    } yield localFiles

  private def findFiles(
      hashService: HashService
  )(implicit c: Config): Task[LocalFiles] = {
    Task
      .foreach(c.sources.paths)(LocalFileStream.findFiles(_, hashService))
      .map(_.foldLeft(LocalFiles())((acc, localFile) => acc ++ localFile))
  }

}

object PlanBuilder extends PlanBuilder
