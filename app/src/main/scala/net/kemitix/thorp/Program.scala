package net.kemitix.thorp

import net.kemitix.thorp.cli.CliArgs
import net.kemitix.thorp.config._
import net.kemitix.thorp.console._
import net.kemitix.thorp.domain.MessageChannel.{
  MessageConsumer,
  MessageSink,
  MessageSupplier
}
import net.kemitix.thorp.domain.StorageEvent.{
  CopyEvent,
  DeleteEvent,
  ErrorEvent,
  UploadEvent
}
import net.kemitix.thorp.domain.{Counters, MessageChannel, StorageEvent}
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
    val executingProgram: MessageSupplier[UIEvent] = execute(configuration)
    val uiChannel = MessageChannel.create(executingProgram)
    uiChannel.addMessageConsumer(UIShell.receiver(configuration))
    uiChannel.startChannel()
    uiChannel.waitForShutdown()
  }

  private def execute(configuration: Configuration) = {
    val uiChannel: MessageSink[UIEvent] = MessageChannel.createSink()
    new Thread(() => {
      showValidConfig(uiChannel)
      val remoteObjects =
        fetchRemoteData(configuration, uiChannel)
      val archive = UnversionedMirrorArchive
      val storageEvents = LocalFileSystem
        .scanCopyUpload(configuration, uiChannel, remoteObjects, archive)
      val deleteEvents = LocalFileSystem
        .scanDelete(configuration, uiChannel, remoteObjects, archive)
      showSummary(uiChannel)(storageEvents ++ deleteEvents)
      uiChannel.shutdown()
    }).start()
    uiChannel
  }

  private def showValidConfig(uiChannel: MessageConsumer[UIEvent]): Unit =
    uiChannel.accept(UIEvent.showValidConfig)

  private def fetchRemoteData(configuration: Configuration,
                              uiChannel: MessageConsumer[UIEvent]) = {
    val bucket = configuration.bucket
    val prefix = configuration.prefix
    val objects = Storage.getInstance().list(bucket, prefix)
    uiChannel.accept(UIEvent.remoteDataFetched(objects.byKey.size))
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
    uiChannel: MessageConsumer[UIEvent]
  )(events: Seq[StorageEvent]): Unit = {
    val counters = events.foldLeft(Counters.empty)(countActivities)
    uiChannel.accept(UIEvent.showSummary(counters))
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
