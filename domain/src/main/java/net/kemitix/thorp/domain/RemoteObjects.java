package net.kemitix.thorp.domain;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RemoteObjects {
    public final MapView<MD5Hash, RemoteKey> byHash;
    public final MapView<RemoteKey, MD5Hash> byKey;
    public static final RemoteObjects empty =
            new RemoteObjects(MapView.empty(), MapView.empty());
    public static RemoteObjects create(
            Map<MD5Hash, RemoteKey> byHash,
            Map<RemoteKey, MD5Hash> byKey
    ) {
        return new RemoteObjects(
                MapView.of(byHash),
                MapView.of(byKey)
        );
    }
    public boolean remoteKeyExists(RemoteKey remoteKey) {
        return byKey.contains(remoteKey);
    }
    public boolean remoteMatchesLocalFile(LocalFile localFile) {
        return byKey.get(localFile.remoteKey)
                .map(localFile::matchesHash)
                .orElse(false);
    }
    public Optional<Tuple<RemoteKey, MD5Hash>> remoteHasHash(Hashes hashes) {
        return byHash.collectFirst(
                (hash, key) -> hashes.values().contains(hash))
                .map(Tuple::swap);
    }
}
