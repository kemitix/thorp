package net.kemitix.thorp.filesystem;

import net.kemitix.thorp.domain.*;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Set;

import scala.jdk.CollectionConverters;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

public class PathCacheTest
        implements WithAssertions {

    @Test
    @DisplayName("create()")
    public void create() {
        //given
        Path path = Paths.get("first", "second");
        Hashes hashes = Hashes.create()
                .withKeyValue(HashType.MD5, MD5HashData.Root.hash);
        Instant now = Instant.now();
        LastModified lastModified = LastModified.at(now);
        FileData fileData = FileData.create(hashes, lastModified);
        //when
        Set<String> result = setAsJava(PathCache.unsafeCreate(path, fileData));
        //then
        assertThat(result).containsExactly(String.join(":",
                HashType.MD5.label, MD5HashData.Root.hashString,
                Long.toString(now.toEpochMilli()), path.toString()
        ));
    }

    public Set<String> setAsJava(scala.collection.immutable.Set<String> set) {
        return CollectionConverters.SetHasAsJava(set).asJava();
    }
}
