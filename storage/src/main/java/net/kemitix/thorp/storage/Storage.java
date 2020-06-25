package net.kemitix.thorp.storage;

import net.kemitix.thorp.domain.*;
import net.kemitix.thorp.uishell.UploadEventListener;

import java.util.ServiceLoader;

public interface Storage {

    RemoteObjects list(
            Bucket bucket,
            RemoteKey prefix
    );

    StorageEvent upload(
            LocalFile localFile,
            Bucket bucket,
            UploadEventListener.Settings listener
    );

    StorageEvent copy(
            Bucket bucket,
            RemoteKey sourceKey,
            MD5Hash hash,
            RemoteKey targetKey
    );

    StorageEvent delete(
            Bucket bucket,
            RemoteKey remoteKey
    );

    void shutdown();

    static Storage getInstance() {
        return ServiceLoader.load(Storage.class).iterator().next();
    }

}
