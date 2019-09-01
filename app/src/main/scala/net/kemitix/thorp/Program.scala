package net.kemitix.thorp

import net.kemitix.eip.zio.MessageChannel
import net.kemitix.thorp.cli.CliArgs
import net.kemitix.thorp.config._
import net.kemitix.thorp.console._
import net.kemitix.thorp.filesystem.{FileSystem, Hasher}
import net.kemitix.thorp.lib.CoreTypes.CoreProgram
import net.kemitix.thorp.lib._
import net.kemitix.thorp.storage.Storage
import net.kemitix.throp.uishell.UIEvent
import zio.{UIO, ZIO}

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
      uiEventReceiver <- uiEventReceiver
      _               <- MessageChannel.pointToPoint(uiEventSender)(uiEventReceiver).runDrain
    } yield ()

  // headless because it shouldn't use any Console effects, only send UIEvents
  // TODO: refactor out Console as a required effect
  private def headlessProgram
    : ZIO[Any,
          Nothing,
          MessageChannel.ESender[
            Console with Storage with Config with FileSystem with Hasher,
            Throwable,
            UIEvent]] = UIO { channel =>
    for {
      _          <- SyncLogging.logRunStart
      remoteData <- fetchRemoteData
      syncPlan   <- PlanBuilder.createPlan(remoteData)
      archive    <- UIO(UnversionedMirrorArchive)
      events     <- PlanExecutor.executePlan(archive, syncPlan)
      _          <- SyncLogging.logRunFinished(events)
    } yield ()
  }

  private def uiEventReceiver
    : ZIO[Any, Nothing, MessageChannel.UReceiver[Console, UIEvent]] = UIO {
    message =>
      UIO.succeed(())
  }

  private def fetchRemoteData =
    for {
      bucket  <- Config.bucket
      prefix  <- Config.prefix
      objects <- Storage.list(bucket, prefix)
      _       <- Console.putStrLn(s"Found ${objects.byKey.size} remote objects")
    } yield objects

  private def handleErrors(throwable: Throwable) =
    Console.putStrLn("There were errors:") *> logValidationErrors(throwable)

  private def logValidationErrors(throwable: Throwable) =
    throwable match {
      case ConfigValidationException(errors) =>
        ZIO.foreach_(errors)(error => Console.putStrLn(s"- $error"))
    }

}

object Program extends Program
