package net.kemitix.thorp.lib;

import net.kemitix.thorp.config.Configuration;
import net.kemitix.thorp.domain.*;
import net.kemitix.thorp.domain.channel.Channel;
import net.kemitix.thorp.filesystem.Resource;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileScannerTest
        implements WithAssertions {

    @Test
    @DisplayName("scan resources as source")
    public void scanResources() throws InterruptedException {
        //given
        File source = Resource.select(this, "upload").toFile();
        Configuration configuration = Configuration.create()
                .withSources(Sources.create(
                        Collections.singletonList(
                                source.toPath())));
        List<LocalFile> localFiles = new ArrayList<>();
        //when
        Channel.<LocalFile>create("test-file-scan")
                .addListener(localFiles::add)
                .run(sink -> FileScanner.scanSources(configuration, sink))
                .start()
                .waitForShutdown();
        //then
        File rootFile = source.toPath().resolve("root-file").toFile();
        File leafFile = source.toPath().resolve("subdir/leaf-file").toFile();
        Hashes rootHashes = Hashes.create(HashType.MD5, MD5HashData.Root.hash);
        Hashes leafHashes = Hashes.create(HashType.MD5, MD5HashData.Leaf.hash);
        RemoteKey rootKey = RemoteKey.create("root-file");
        RemoteKey leafKey = RemoteKey.create("subdir/leaf-file");
        long rootLength = 55L;
        long leafLength = 58L;
        assertThat(localFiles)
                .containsExactlyInAnyOrder(
                        LocalFile.create(rootFile, source, rootHashes, rootKey, rootLength),
                        LocalFile.create(leafFile, source, leafHashes, leafKey, leafLength));
    }
}
