package net.kemitix.thorp.lib;

import net.kemitix.thorp.config.Configuration;
import net.kemitix.thorp.console.Console;
import net.kemitix.thorp.console.ConsoleOut;
import net.kemitix.thorp.domain.RemoteKey;
import net.kemitix.thorp.domain.StorageEvent;
import net.kemitix.thorp.domain.channel.Sink;
import net.kemitix.thorp.uishell.UIEvent;

public interface Archive {

    StorageEvent update(
            Configuration configuration,
            Sink<UIEvent> uiSink,
            SequencedAction sequencedAction,
            long totalBytesSoFar);

    default StorageEvent logEvent(
            Configuration configuration,
            StorageEvent event
    ) {
        boolean batchMode = configuration.batchMode;

        if (event instanceof StorageEvent.UploadEvent) {
            RemoteKey remoteKey = ((StorageEvent.UploadEvent) event).remoteKey;
            Console.putMessageLnB(
                    ConsoleOut.uploadComplete(remoteKey), batchMode);
        } else if (event instanceof StorageEvent.CopyEvent) {
            StorageEvent.CopyEvent copyEvent = (StorageEvent.CopyEvent) event;
            RemoteKey sourceKey = copyEvent.sourceKey;
            RemoteKey targetKey = copyEvent.targetKey;
            Console.putMessageLnB(
                    ConsoleOut.copyComplete(sourceKey, targetKey), batchMode);
        } else if (event instanceof StorageEvent.DeleteEvent) {
            RemoteKey remoteKey = ((StorageEvent.DeleteEvent) event).remoteKey;
            Console.putMessageLnB(
                    ConsoleOut.deleteComplete(remoteKey), batchMode);
        } else if (event instanceof StorageEvent.ErrorEvent) {
            StorageEvent.ErrorEvent errorEvent = (StorageEvent.ErrorEvent) event;
            StorageEvent.ActionSummary action = errorEvent.action;
            Throwable e = errorEvent.e;
            Console.putMessageLnB(
                    ConsoleOut.errorQueueEventOccurred(action, e), batchMode);
        }

        return event;
    }
}
