package net.kemitix.thorp.domain;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Optional;

@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LocalFile {
    public final File file;
    public final File source;
    public final Hashes hashes;
    public final RemoteKey remoteKey;
    public final Long length;
    public static LocalFile create(
            File file,
            File source,
            Hashes hashes,
            RemoteKey remoteKey,
            Long length
    ) {
        return new LocalFile(file, source, hashes, remoteKey, length);
    }
    public boolean matchesHash(MD5Hash hash) {
        return hashes.values().contains(hash);
    }
    public Optional<String> md5base64() {
        return hashes.get(HashType.MD5).map(MD5Hash::hash64);
    }
}
