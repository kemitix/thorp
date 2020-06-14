package net.kemitix.thorp.filesystem;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.kemitix.thorp.domain.Hashes;
import net.kemitix.thorp.domain.LastModified;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class FileData {
    public final Hashes hashes;
    public final LastModified lastModified;
    public static FileData create(Hashes hashes, LastModified lastModified) {
        return new FileData(hashes, lastModified);
    }
    public FileData join(FileData other) {
        return FileData.create(
                hashes.merge(other.hashes),
                lastModified // discards other.lastModified
        );
    }
}
