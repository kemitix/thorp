package net.kemitix.thorp

import net.kemitix.eip.zio.MessageChannel.UChannel
import net.kemitix.eip.zio.{Message, MessageChannel}
import net.kemitix.thorp.cli.CliArgs
import net.kemitix.thorp.config._
import net.kemitix.thorp.console._
import net.kemitix.thorp.domain.{Counters, StorageQueueEvent}
import net.kemitix.thorp.domain.StorageQueueEvent.{
  CopyQueueEvent,
  DeleteQueueEvent,
  ErrorQueueEvent,
  UploadQueueEvent
}
import net.kemitix.thorp.filesystem.{FileSystem, Hasher}
import net.kemitix.thorp.lib.CoreTypes.CoreProgram
import net.kemitix.thorp.lib._
import net.kemitix.thorp.storage.Storage
import net.kemitix.throp.uishell.{UIEvent, UIShell}
import zio.clock.Clock
import zio.{RIO, UIO, ZIO}

trait Program {

  lazy val version = s"Thorp v${thorp.BuildInfo.version}"

  def run(args: List[String]): CoreProgram[Unit] = {
    for {
      cli    <- CliArgs.parse(args)
      config <- ConfigurationBuilder.buildConfig(cli)
      _      <- Config.set(config)
      _      <- ZIO.when(showVersion(cli))(Console.putStrLn(version))
      _      <- ZIO.when(!showVersion(cli))(execute.catchAll(handleErrors))
    } yield ()
  }

  private def showVersion: ConfigOptions => Boolean =
    cli => ConfigQuery.showVersion(cli)

  private def execute =
    for {
      uiEventSender   <- headlessProgram
      uiEventReceiver <- UIShell.receiver
      _               <- MessageChannel.pointToPoint(uiEventSender)(uiEventReceiver).runDrain
    } yield ()

  type UIChannel = UChannel[Any, UIEvent]

  // headless because it shouldn't use any Console effects, only send UIEvents
  // TODO: refactor out Console as a required effect
  private def headlessProgram: ZIO[
    Any,
    Nothing,
    MessageChannel.ESender[
      Console with Storage with Config with FileSystem with Hasher with Clock,
      Throwable,
      UIEvent]] = UIO { uiChannel =>
    (for {
      _          <- showValidConfig(uiChannel)
      remoteData <- fetchRemoteData(uiChannel)
      archive    <- UIO(UnversionedMirrorArchive)
      syncPlan <- PlanBuilder.createPlan(remoteData)
      events   <- PlanExecutor.executePlan(archive, syncPlan)
      _        <- showSummary(uiChannel)(events)
    } yield ()) <* MessageChannel.endChannel(uiChannel)
  }

  private def showValidConfig(uiChannel: UIChannel) =
    Message.create(UIEvent.ShowValidConfig) >>= MessageChannel.send(uiChannel)

  private def fetchRemoteData(uiChannel: UIChannel) =
    for {
      bucket  <- Config.bucket
      prefix  <- Config.prefix
      objects <- Storage.list(bucket, prefix)
      _ <- Message.create(UIEvent.RemoteDataFetched(objects.byKey.size)) >>= MessageChannel
        .send(uiChannel)
    } yield objects

  private def handleErrors(throwable: Throwable) =
    Console.putStrLn("There were errors:") *> logValidationErrors(throwable)

  private def logValidationErrors(throwable: Throwable) =
    throwable match {
      case ConfigValidationException(errors) =>
        ZIO.foreach_(errors)(error => Console.putStrLn(s"- $error"))
    }

  private def showSummary(uiChannel: UIChannel)(
      events: Seq[StorageQueueEvent]): RIO[Clock, Unit] = {
    val counters = events.foldLeft(Counters.empty)(countActivities)
    Message.create(UIEvent.ShowSummary(counters)) >>= MessageChannel.send(
      uiChannel)
  }

  private def countActivities: (Counters, StorageQueueEvent) => Counters =
    (counters: Counters, s3Action: StorageQueueEvent) => {
      import Counters._
      val increment: Int => Int = _ + 1
      s3Action match {
        case _: UploadQueueEvent => uploaded.modify(increment)(counters)
        case _: CopyQueueEvent   => copied.modify(increment)(counters)
        case _: DeleteQueueEvent => deleted.modify(increment)(counters)
        case _: ErrorQueueEvent  => errors.modify(increment)(counters)
        case _                   => counters
      }
    }

}

object Program extends Program
