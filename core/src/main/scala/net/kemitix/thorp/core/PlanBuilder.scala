package net.kemitix.thorp.core

import cats.data.{EitherT, NonEmptyChain}
import cats.effect.IO
import cats.implicits._
import net.kemitix.thorp.core.Action.DoNothing
import net.kemitix.thorp.domain._
import net.kemitix.thorp.storage.api.{HashService, StorageService}

trait PlanBuilder {

  def createPlan(storageService: StorageService,
            hashService: HashService,
            configOptions: ConfigOptions)
           (implicit l: Logger): EitherT[IO, List[String], SyncPlan] =
    EitherT(ConfigurationBuilder.buildConfig(configOptions))
      .leftMap(errorMessages)
      .flatMap(config => useValidConfig(storageService, hashService)(config, l))

  def errorMessages(errors: NonEmptyChain[ConfigValidation]): List[String] =
    errors.map(cv => cv.errorMessage).toList

  def removeDoNothing: Action => Boolean = {
    case _: DoNothing => false
    case _ => true
  }

  def assemblePlan(implicit c: Config): ((S3ObjectsData, LocalFiles)) => SyncPlan = {
    case (remoteData, localData) => {
      val actions =
        (actionsForLocalFiles(localData, remoteData) ++
          actionsForRemoteKeys(remoteData))
          .filter(removeDoNothing)
      SyncPlan(
        actions = actions,
        syncTotals = SyncTotals(
          count = localData.count,
          totalSizeBytes = localData.totalSizeBytes))
    }
  }

  def useValidConfig(storageService: StorageService,
                     hashService: HashService)
                    (implicit c: Config, l: Logger): EitherT[IO, List[String], SyncPlan] = {
    for {
      _ <- EitherT.liftF(SyncLogging.logRunStart(c.bucket, c.prefix, c.sources))
      actions <- gatherMetadata(storageService, hashService)
        .leftMap(error => List(error))
        .map(assemblePlan)
    } yield actions
  }

  private def gatherMetadata(storageService: StorageService,
                             hashService: HashService)
                            (implicit l: Logger,
                             c: Config): EitherT[IO, String, (S3ObjectsData, LocalFiles)] =
    for {
      remoteData <- fetchRemoteData(storageService)
      localData <- EitherT.liftF(findLocalFiles(hashService))
    } yield (remoteData, localData)

  private def actionsForLocalFiles(localData: LocalFiles, remoteData: S3ObjectsData)
                                  (implicit c: Config) =
    localData.localFiles.foldLeft(Stream[Action]())((acc, lf) => createActionFromLocalFile(lf, remoteData, acc) ++ acc)

  private def actionsForRemoteKeys(remoteData: S3ObjectsData)
                                  (implicit c: Config) =
    remoteData.byKey.keys.foldLeft(Stream[Action]())((acc, rk) => createActionFromRemoteKey(rk) #:: acc)

  private def fetchRemoteData(storageService: StorageService)
                             (implicit c: Config, l: Logger) =
    storageService.listObjects(c.bucket, c.prefix)

  private def findLocalFiles(hashService: HashService)
                            (implicit config: Config, l: Logger) =
    for {
      _ <- SyncLogging.logFileScan
      localFiles <- findFiles(hashService)
    } yield localFiles

  private def findFiles(hashService: HashService)
                       (implicit c: Config, l: Logger): IO[LocalFiles] = {
    val ioListLocalFiles = (for {
      source <- c.sources.paths
    } yield LocalFileStream.findFiles(source, hashService)).sequence
    for {
      listLocalFiles <- ioListLocalFiles
      localFiles = listLocalFiles.foldRight(LocalFiles()){
        (acc, moreLocalFiles) => {
          acc ++ moreLocalFiles
        }
      }
    } yield localFiles
  }

  private def createActionFromLocalFile(lf: LocalFile,
                                        remoteData: S3ObjectsData,
                                        previousActions: Stream[Action])
                                       (implicit c: Config) =
    ActionGenerator.createActions(S3MetaDataEnricher.getMetadata(lf, remoteData), previousActions)

  private def createActionFromRemoteKey(rk: RemoteKey)
                                       (implicit c: Config) =
    if (rk.isMissingLocally(c.sources, c.prefix)) Action.ToDelete(c.bucket, rk, 0L)
    else DoNothing(c.bucket, rk, 0L)

}

object PlanBuilder extends PlanBuilder
