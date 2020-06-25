package net.kemitix.thorp

import net.kemitix.thorp.cli.CliArgs
import net.kemitix.thorp.config._
import net.kemitix.thorp.console._
import net.kemitix.thorp.domain.StorageEvent.{
  CopyEvent,
  DeleteEvent,
  ErrorEvent,
  UploadEvent
}
import net.kemitix.thorp.domain.{Channel, Counters, StorageEvent}
import net.kemitix.thorp.lib.{LocalFileSystem, UnversionedMirrorArchive}
import net.kemitix.thorp.storage.Storage
import net.kemitix.thorp.uishell.{UIEvent, UIShell}

import scala.io.AnsiColor.{RESET, WHITE}
import scala.jdk.CollectionConverters._

trait Program {

  val version = "0.11.0"
  lazy val versionLabel = s"${WHITE}Thorp v$version$RESET"

  def run(args: List[String]): Unit = {
    val cli = CliArgs.parse(args.toArray)
    val config = ConfigurationBuilder.buildConfig(cli)
    Console.putStrLn(versionLabel)
    if (!showVersion(cli)) {
      executeWithUI(config)
    }
  }

  private def showVersion: ConfigOptions => Boolean =
    cli => ConfigQuery.showVersion(cli)

  private def executeWithUI(configuration: Configuration): Unit = {
    val uiChannel: Channel[UIEvent] = Channel.create("thorp-ui")
    uiChannel.addListener(UIShell.listener(configuration))
    uiChannel.run(sink => execute(configuration, sink), "thorp-main")
    uiChannel.start()
    uiChannel.waitForShutdown()
  }

  private def execute(configuration: Configuration,
                      uiSink: Channel.Sink[UIEvent]) = {
    showValidConfig(uiSink)
    val remoteObjects =
      fetchRemoteData(configuration, uiSink)
    val archive = UnversionedMirrorArchive
    val storageEvents = LocalFileSystem
      .scanCopyUpload(configuration, uiSink, remoteObjects, archive)
    val deleteEvents = LocalFileSystem
      .scanDelete(configuration, uiSink, remoteObjects, archive)
    showSummary(uiSink)(storageEvents ++ deleteEvents)
    uiSink.shutdown();
  }

  private def showValidConfig(uiSink: Channel.Sink[UIEvent]): Unit =
    uiSink.accept(UIEvent.showValidConfig)

  private def fetchRemoteData(configuration: Configuration,
                              uiSink: Channel.Sink[UIEvent]) = {
    val bucket = configuration.bucket
    val prefix = configuration.prefix
    val objects = Storage.getInstance().list(bucket, prefix)
    uiSink.accept(UIEvent.remoteDataFetched(objects.byKey.size))
    objects
  }

  //TODO not called
  private def logValidationErrors(throwable: Throwable) =
    throwable match {
      case validateError: ConfigValidationException =>
        validateError.getErrors.asScala
          .map(error => Console.putStrLn(s"- $error"))
    }

  private def showSummary(
    uiSink: Channel.Sink[UIEvent]
  )(events: Seq[StorageEvent]): Unit = {
    val counters = events.foldLeft(Counters.empty)(countActivities)
    uiSink.accept(UIEvent.showSummary(counters))
  }

  private def countActivities =
    (counters: Counters, s3Action: StorageEvent) => {
      s3Action match {
        case _: UploadEvent => counters.incrementUploaded()
        case _: CopyEvent   => counters.incrementCopied()
        case _: DeleteEvent => counters.incrementDeleted()
        case _: ErrorEvent  => counters.incrementErrors()
        case _              => counters
      }
    }
}

object Program extends Program
