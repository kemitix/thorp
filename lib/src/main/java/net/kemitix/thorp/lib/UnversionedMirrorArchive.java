package net.kemitix.thorp.lib;

import net.kemitix.thorp.config.Configuration;
import net.kemitix.thorp.domain.*;
import net.kemitix.thorp.domain.channel.Sink;
import net.kemitix.thorp.storage.Storage;
import net.kemitix.thorp.uishell.UIEvent;
import net.kemitix.thorp.uishell.UploadEventListener;

public class UnversionedMirrorArchive implements Archive {

    public static Archive create() {
        return new UnversionedMirrorArchive();
    }

    @Override
    public StorageEvent update(
            Configuration configuration,
            Sink<UIEvent> uiSink,
            SequencedAction sequencedAction,
            long totalBytesSoFar
    ) {
        Action action = sequencedAction.action();
        int index = sequencedAction.index();
        Bucket bucket = action.bucket;
        if (action instanceof Action.ToUpload) {
            LocalFile localFile = ((Action.ToUpload) action).localFile;
            return Storage.getInstance()
                    .upload(localFile, bucket,
                            UploadEventListener.settings(
                                    uiSink, localFile, index, totalBytesSoFar,
                                    configuration.batchMode));
        } else if(action instanceof Action.ToCopy) {
            Action.ToCopy toCopy = (Action.ToCopy) action;
            RemoteKey sourceKey = toCopy.sourceKey;
            MD5Hash hash = toCopy.hash;
            RemoteKey targetKey = toCopy.targetKey;
            return Storage.getInstance()
                    .copy(bucket, sourceKey, hash, targetKey);
        } else if(action instanceof Action.ToDelete) {
            RemoteKey remoteKey = action.remoteKey;
            try {
                return Storage.getInstance().delete(bucket, remoteKey);
            } catch (Throwable e) {
                return StorageEvent.errorEvent(
                        StorageEvent.ActionSummary.delete(remoteKey.key()),
                        remoteKey, e);
            }
        } else {
            RemoteKey remoteKey = action.remoteKey;
            return StorageEvent.doNothingEvent(remoteKey);
        }
    }

}
