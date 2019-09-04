package net.kemitix.throp.uishell

import net.kemitix.eip.zio.MessageChannel
import net.kemitix.thorp.config.Config
import net.kemitix.thorp.console.ConsoleOut.{
  CopyComplete,
  DeleteComplete,
  ErrorQueueEventOccurred,
  UploadComplete
}
import net.kemitix.thorp.console.{Console, ConsoleOut}
import net.kemitix.thorp.domain.StorageQueueEvent._
import net.kemitix.thorp.domain.Terminal.eraseToEndOfScreen
import zio.{UIO, ZIO}

object UIShell {
  def receiver: UIO[MessageChannel.UReceiver[Console with Config, UIEvent]] =
    UIO { uiEventMessage =>
      uiEventMessage.body match {

        case UIEvent.ShowValidConfig =>
          for {
            bucket  <- Config.bucket
            prefix  <- Config.prefix
            sources <- Config.sources
            _ <- Console.putMessageLn(
              ConsoleOut.ValidConfig(bucket, prefix, sources))
          } yield ()

        case UIEvent.RemoteDataFetched(size) =>
          Console.putStrLn(s"Found $size remote objects")

        case UIEvent.ShowSummary(counters) =>
          Console.putStrLn(eraseToEndOfScreen) *>
            Console.putStrLn(s"Uploaded ${counters.uploaded} files") *>
            Console.putStrLn(s"Copied   ${counters.copied} files") *>
            Console.putStrLn(s"Deleted  ${counters.deleted} files") *>
            Console.putStrLn(s"Errors   ${counters.errors}")

        case UIEvent.FileFound(localFile) =>
          for {
            batchMode <- Config.batchMode
            _ <- ZIO.when(batchMode)(
              Console.putStrLn(s"Found: ${localFile.file}"))
          } yield ()

        case UIEvent.ActionChosen(action) =>
          UIO(()) //Console.putStrLn(s"Action: ${action.toString}")

        case UIEvent.AwaitingAnotherUpload(remoteKey, hash) =>
          Console.putStrLn(
            s"Awaiting another upload of $hash before copying it to $remoteKey")

        case UIEvent.AnotherUploadWaitComplete(action) =>
          Console.putStrLn(s"Finished waiting to other upload - now $action")

        case UIEvent.ActionFinished(event, actionCounter, bytesCounter) =>
          for {
            batchMode <- Config.batchMode
            _ <- event match {
              case UploadQueueEvent(remoteKey, _) =>
                Console.putMessageLnB(UploadComplete(remoteKey), batchMode)
              case CopyQueueEvent(sourceKey, targetKey) =>
                Console.putMessageLnB(CopyComplete(sourceKey, targetKey),
                                      batchMode)
              case DeleteQueueEvent(remoteKey) =>
                Console.putMessageLnB(DeleteComplete(remoteKey), batchMode)
              case ErrorQueueEvent(action, _, e) =>
                Console.putMessageLnB(ErrorQueueEventOccurred(action, e),
                                      batchMode)
              case DoNothingQueueEvent(_) => UIO(())
              case ShutdownQueueEvent()   => UIO(())
            }
          } yield ()
      }
    }
}
