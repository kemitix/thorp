package net.kemitix.thorp.core

import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console.Console
import net.kemitix.thorp.domain.StorageQueueEvent
import net.kemitix.thorp.storage.api.Storage
import zio.{Ref, ZIO}

trait PlanExecutor {

  def executePlan(
      archive: ThorpArchive,
      syncPlan: SyncPlan
  ): ZIO[Storage with Config with Console,
         Throwable,
         Seq[
           StorageQueueEvent
         ]] =
    for {
      actionCounter <- Ref.make(0)
      bytesCounter  <- Ref.make(0L)
      events        <- applyActions(archive, syncPlan, actionCounter, bytesCounter)
    } yield events

  private def applyActions(
      archive: ThorpArchive,
      syncPlan: SyncPlan,
      actionCounter: Ref[Int],
      bytesCounter: Ref[Long]
  ): ZIO[Storage with Console with Config,
         Throwable,
         Stream[StorageQueueEvent]] = {
    ZIO.foldLeft(syncPlan.actions)(Stream.empty[StorageQueueEvent]) {
      (stream: Stream[StorageQueueEvent], action) =>
        val result: ZIO[Storage with Console with Config,
                        Throwable,
                        StorageQueueEvent] =
          updateArchive(archive, actionCounter, bytesCounter)(action)
        result.map(event => event #:: stream)
    }
  }

  private def updateArchive(archive: ThorpArchive,
                            actionCounterRef: Ref[Int],
                            bytesCounterRef: Ref[Long])(action: Action) =
    for {
      actionCounter <- actionCounterRef.update(_ + 1)
      bytesCounter  <- bytesCounterRef.update(_ + action.size)
      event <- archive.update(SequencedAction(action, actionCounter),
                              bytesCounter)
    } yield event

}

object PlanExecutor extends PlanExecutor
