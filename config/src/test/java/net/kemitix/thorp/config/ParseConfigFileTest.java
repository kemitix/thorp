package net.kemitix.thorp.config;

import net.kemitix.thorp.filesystem.TemporaryFolder;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

public class ParseConfigFileTest
        implements WithAssertions, TemporaryFolder {

    @Test
    @DisplayName("when file is missing then no options")
    public void whenFileMissing_thenNoOptions() throws IOException {
        assertThat(invoke(new File("/path/to/missing/file")))
                .isEqualTo(ConfigOptions.empty());
    }
    @Test
    @DisplayName("when file is empty then no options")
    public void whenEmptyFile_thenNoOptions() {
        withDirectory(dir -> {
            File file = createFile(dir, "empty-file", Collections.emptyList());
            assertThat(invoke(file)).isEqualTo(ConfigOptions.empty());
        });
    }
    @Test
    @DisplayName("when no valid entried then no options")
    public void whenNoValidEntries_thenNoOptions() {
        withDirectory(dir -> {
            File file = createFile(dir, "invalid-config",
                    Arrays.asList("no valid = config items", "invalid line"));
            assertThat(invoke(file)).isEqualTo(ConfigOptions.empty());
        });
    }

    @Test
    @DisplayName("when file is valid then parse options")
    public void whenValidFile_thenOptions() {
        withDirectory(dir -> {
            File file = createFile(dir, "simple-config", Arrays.asList(
                    "source = /path/to/source",
                    "bucket = bucket-name"));
            assertThat(invoke(file)).isEqualTo(
                    ConfigOptions.create(
                            Arrays.asList(
                                    ConfigOption.source(Paths.get("/path/to/source")),
                                    ConfigOption.bucket("bucket-name"))));
        });
    }

    ConfigOptions invoke(File file) {
        try {
            return ParseConfigFile.parseFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
