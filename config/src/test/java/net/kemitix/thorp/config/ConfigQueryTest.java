package net.kemitix.thorp.config;

import net.kemitix.thorp.domain.Sources;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConfigQueryTest
        implements WithAssertions {
    @Nested
    @DisplayName("show version")
    public class ShowVersionTest{
        @Test
        @DisplayName("when set then show")
        public void whenSet_thenShow() {
            assertThat(ConfigQuery.showVersion(
                    ConfigOptions.create(
                            Collections.singletonList(
                                    ConfigOption.version()))
            )).isTrue();
        }
        @Test
        @DisplayName("when not set then do not show")
        public void whenNotSet_thenDoNotShow() {
            assertThat(ConfigQuery.showVersion(
                    ConfigOptions.create(
                            Collections.emptyList())
            )).isFalse();
        }
    }
    @Nested
    @DisplayName("batch mode")
    public class BatchModeTest{
        @Test
        @DisplayName("when set then show")
        public void whenSet_thenShow() {
            assertThat(ConfigQuery.batchMode(
                    ConfigOptions.create(
                            Collections.singletonList(
                                    ConfigOption.batchMode()))
            )).isTrue();
        }
        @Test
        @DisplayName("when not set then do not show")
        public void whenNotSet_thenDoNotShow() {
            assertThat(ConfigQuery.batchMode(
                    ConfigOptions.create(
                            Collections.emptyList())
            )).isFalse();
        }
    }
    @Nested
    @DisplayName("ignore user options")
    public class IgnoreUserOptionsTest{
        @Test
        @DisplayName("when set then show")
        public void whenSet_thenShow() {
            assertThat(ConfigQuery.ignoreUserOptions(
                    ConfigOptions.create(
                            Collections.singletonList(
                                    ConfigOption.ignoreUserOptions()))
            )).isTrue();
        }
        @Test
        @DisplayName("when not set then do not show")
        public void whenNotSet_thenDoNotShow() {
            assertThat(ConfigQuery.ignoreUserOptions(
                    ConfigOptions.create(
                            Collections.emptyList())
            )).isFalse();
        }
    }
    @Nested
    @DisplayName("ignore global options")
    public class IgnoreGlobalOptionsTest{
        @Test
        @DisplayName("when set then show")
        public void whenSet_thenShow() {
            assertThat(ConfigQuery.ignoreGlobalOptions(
                    ConfigOptions.create(
                            Collections.singletonList(
                                    ConfigOption.ignoreGlobalOptions()))
            )).isTrue();
        }
        @Test
        @DisplayName("when not set then do not show")
        public void whenNotSet_thenDoNotShow() {
            assertThat(ConfigQuery.ignoreGlobalOptions(
                    ConfigOptions.create(
                            Collections.emptyList())
            )).isFalse();
        }
    }
    @Nested
    @DisplayName("source")
    public class SourcesTest {
        Path pathA = Paths.get("a-path");
        Path pathB = Paths.get("b-path");
        @Test
        @DisplayName("when not set then use current directory")
        public void whenNoSet_thenCurrentDir() {
            Sources expected = Sources.create(
                    Collections.singletonList(
                            Paths.get(
                                    System.getenv("PWD")
                            )));
            assertThat(ConfigQuery.sources(ConfigOptions.empty()))
                    .isEqualTo(expected);
        }
        @Test
        @DisplayName("when one source then have one source")
        public void whenOneSource_thenOneSource() {
            List<Path> expected = Collections.singletonList(pathA);
            assertThat(ConfigQuery.sources(
                    ConfigOptions.create(
                            Collections.singletonList(
                                    ConfigOption.source(pathA)))).paths())
                    .isEqualTo(expected);
        }
        @Test
        @DisplayName("when two sources then have two sources")
        public void whenTwoSources_thenTwoSources() {
            List<Path> expected = Arrays.asList(pathA, pathB);
            assertThat(
                    ConfigQuery.sources(
                            ConfigOptions.create(
                                    Arrays.asList(
                                            ConfigOption.source(pathA),
                                            ConfigOption.source(pathB))
                            )).paths())
                    .isEqualTo(expected);
        }
    }
}
