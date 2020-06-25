package net.kemitix.thorp.uishell;

import net.kemitix.thorp.config.Configuration;
import net.kemitix.thorp.console.Console;
import net.kemitix.thorp.console.ConsoleOut;
import net.kemitix.thorp.domain.*;

import static net.kemitix.thorp.domain.Terminal.eraseLineForward;
import static net.kemitix.thorp.domain.Terminal.eraseToEndOfScreen;

public interface UIShell {
    static Channel.Listener<UIEvent> listener(Configuration configuration) {
        return new Channel.Listener<UIEvent>() {
            @Override
            public void accept(UIEvent uiEvent) {
                if (uiEvent instanceof UIEvent.RequestCycle)
                    requestCycle((UIEvent.RequestCycle) uiEvent, configuration);
                else if (uiEvent instanceof UIEvent.FileFound)
                    fileFound((UIEvent.FileFound) uiEvent, configuration);
                else if (uiEvent instanceof UIEvent.ActionChosen)
                    actionChosen((UIEvent.ActionChosen) uiEvent, configuration);
                else if (uiEvent instanceof UIEvent.AwaitingAnotherUpload)
                    awaitingAnotherUpload((UIEvent.AwaitingAnotherUpload) uiEvent);
                else if (uiEvent instanceof UIEvent.AnotherUploadWaitComplete)
                    anotherUploadWaitComplete((UIEvent.AnotherUploadWaitComplete) uiEvent);
                else if (uiEvent instanceof UIEvent.ActionFinished)
                    actionFinished((UIEvent.ActionFinished) uiEvent, configuration);
                else if (uiEvent instanceof UIEvent.RemoteDataFetched)
                    remoteDataFetched((UIEvent.RemoteDataFetched) uiEvent);
                else if (uiEvent instanceof UIEvent.ShowSummary)
                    showSummary((UIEvent.ShowSummary) uiEvent);
                else if (uiEvent instanceof UIEvent.ShowValidConfig)
                    showValidConfig(configuration);
            }

            private void requestCycle(
                    UIEvent.RequestCycle uiEvent,
                    Configuration configuration
            ) {
                ProgressUI.requestCycle(
                        configuration,
                        uiEvent.localFile,
                        uiEvent.bytesTransferred,
                        uiEvent.index,
                        uiEvent.totalBytesSoFar
                        );
            }

            private void actionFinished(
                    UIEvent.ActionFinished uiEvent,
                    Configuration configuration
            ) {
                StorageEvent storageEvent = uiEvent.event;
                if (storageEvent instanceof StorageEvent.CopyEvent)
                    copyActionFinished(
                            (StorageEvent.CopyEvent) storageEvent,
                            configuration);
                else if (storageEvent instanceof StorageEvent.UploadEvent)
                    uploadActionFinished(
                            (StorageEvent.UploadEvent) storageEvent,
                            configuration);
                else if (storageEvent instanceof StorageEvent.DeleteEvent)
                    deleteActionFinished(
                            (StorageEvent.DeleteEvent) storageEvent,
                            configuration);
                else if (storageEvent instanceof StorageEvent.ErrorEvent)
                    errorActionFinished(
                            (StorageEvent.ErrorEvent) storageEvent,
                            configuration);
            }

            private void errorActionFinished(
                    StorageEvent.ErrorEvent errorEvent,
                    Configuration configuration
            ) {
                RemoteKey remoteKey = errorEvent.remoteKey;
                StorageEvent.ActionSummary action = errorEvent.action;
                Throwable e = errorEvent.e;
                ProgressUI.finishedUploading(remoteKey);
                Console.putMessageLnB(
                        ConsoleOut.errorQueueEventOccurred(action, e),
                        configuration.batchMode);
            }

            private void deleteActionFinished(
                    StorageEvent.DeleteEvent deleteEvent,
                    Configuration configuration
            ) {
                RemoteKey remoteKey = deleteEvent.remoteKey;
                Console.putMessageLnB(
                        ConsoleOut.deleteComplete(remoteKey),
                        configuration.batchMode);
            }

            private void uploadActionFinished(
                    StorageEvent.UploadEvent uploadEvent,
                    Configuration configuration
            ) {
                RemoteKey remoteKey = uploadEvent.remoteKey;
                Console.putMessageLnB(
                        ConsoleOut.uploadComplete(remoteKey),
                        configuration.batchMode);
                ProgressUI.finishedUploading(remoteKey);
            }

            private void copyActionFinished(
                    StorageEvent.CopyEvent copyEvent,
                    Configuration configuration
            ) {
                RemoteKey sourceKey = copyEvent.sourceKey;
                RemoteKey targetKey = copyEvent.targetKey;
                Console.putMessageLnB(
                        ConsoleOut.copyComplete(sourceKey, targetKey),
                        configuration.batchMode);
            }

            private void anotherUploadWaitComplete(
                    UIEvent.AnotherUploadWaitComplete uiEvent
            ) {
                Console.putStrLn(String.format(
                        "Finished waiting to other upload - now %s",
                        uiEvent.action));
            }

            private void awaitingAnotherUpload(
                    UIEvent.AwaitingAnotherUpload uiEvent
            ) {
                Console.putStrLn(String.format(
                        "Awaiting another upload of %s before copying it to %s",
                        uiEvent.hash, uiEvent.remoteKey));
            }

            private void actionChosen(
                    UIEvent.ActionChosen uiEvent,
                    Configuration configuration
            ) {
                Action action = uiEvent.action;
                if (configuration.batchMode)
                    Console.putStrLn(action.asString());
                else if (!(action instanceof Action.DoNothing)) {
                    Console.putStr(String.format("%s%s\r",
                            action.asString(),
                            eraseLineForward));
                }
            }

            private void fileFound(
                    UIEvent.FileFound uiEvent,
                    Configuration configuration
            ) {
                if (configuration.batchMode) {
                    Console.putStrLn(String.format(
                            "Found: %s", uiEvent.localFile.file));
                }
            }

            private void showSummary(
                    UIEvent.ShowSummary uiEvent
            ) {
                Console.putStrLn(eraseToEndOfScreen);
                Counters counters = uiEvent.counters;
                Console.putStrLn(String.format("Uploaded %d files",
                        counters.uploaded));
                Console.putStrLn(String.format("Copied   %d files",
                        counters.copied));
                Console.putStrLn(String.format("Deleted  %d files",
                        counters.deleted));
                Console.putStrLn(String.format("Errors   %d",
                        counters.errors));
            }

            private void remoteDataFetched(
                    UIEvent.RemoteDataFetched uiEvent
            ) {
                Console.putStrLn(String.format("Found %d remote objects",
                        uiEvent.size));
            }

            private void showValidConfig(
                    Configuration configuration
            ) {
                Console.putMessageLn(
                        ConsoleOut.validConfig(
                                configuration.bucket,
                                configuration.prefix,
                                configuration.sources));
            }
        };

        //  def trimHead(str: String): String = {
        //    val width = Terminal.width
        //    str.length match {
        //      case l if l > width => str.substring(l - width)
        //      case _              => str
        //    }
        //  }
    }
}
