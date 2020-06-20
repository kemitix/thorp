package net.kemitix.thorp.config;

import net.kemitix.thorp.domain.Bucket;
import net.kemitix.thorp.domain.RemoteKey;
import net.kemitix.thorp.filesystem.TemporaryFolder;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConfigurationBuilderTest
        implements WithAssertions {
    Path pwd = Paths.get(System.getenv("PWD"));
    Bucket aBucket = Bucket.named("aBucket");
    ConfigOption coBucket = ConfigOption.bucket(aBucket.name());
    String thorpConfigFileName = ".thorp.conf";
    ConfigOptions configOptions(List<ConfigOption> options) {
        List<ConfigOption> optionList = new ArrayList<>(options);
        optionList.add(ConfigOption.ignoreUserOptions());
        optionList.add(ConfigOption.ignoreGlobalOptions());
        return ConfigOptions.create(optionList);
    }
    @Test
    @DisplayName("when no source then user current directory")
    public void whenNoSource_thenUseCurrentDir() throws IOException, ConfigValidationException {
        Configuration result = ConfigurationBuilder.buildConfig(
                configOptions(Collections.singletonList(coBucket)));
        assertThat(result.sources.paths()).containsExactly(pwd);
    }
    @Nested
    @DisplayName("default source")
    public class DefaultSourceTests {
        @Nested
        @DisplayName("with .thorp.conf")
        public class WithThorpConfTests implements TemporaryFolder {
            @Test
            @DisplayName("with settings")
            public void WithSettingsTests() {
                withDirectory(source -> {
                    //given
                    List<String> settings = Arrays.asList(
                            "bucket = a-bucket",
                            "prefix = a-prefix",
                            "include = an-inclusion",
                            "exclude = an-exclusion"
                    );
                    createFile(source, thorpConfigFileName, settings);
                    //when
                    Configuration result =
                            invoke(configOptions(Collections.singletonList(
                                    ConfigOption.source(source))));
                    //then
                    assertThat(result.bucket).isEqualTo(Bucket.named("a-bucket"));
                    assertThat(result.prefix).isEqualTo(RemoteKey.create("a-prefix"));
                    assertThat(result.filters).hasSize(2)
                            .anySatisfy(filter ->
                                    assertThat(filter.predicate()
                                            .test("an-exclusion")).isTrue())
                            .anySatisfy(filter ->
                                    assertThat(filter.predicate()
                                            .test("an-inclusion")).isTrue());
                });
            }
        }
    }
    @Nested
    @DisplayName("single source")
    public class SingleSourceTests implements TemporaryFolder {
        @Test
        @DisplayName("has single source")
        public void hasSingleSource() {
            withDirectory(aSource -> {
                Configuration result =
                        invoke(
                                configOptions(Arrays.asList(
                                        ConfigOption.source(aSource),
                                        coBucket)));
                assertThat(result.sources.paths()).containsExactly(aSource);
            });
        }
    }
    @Nested
    @DisplayName("multiple sources")
    public class MultipleSources implements TemporaryFolder {
        @Test
        @DisplayName("included in order")
        public void hasBothSourcesInOrder() {
            withDirectory(currentSource -> {
                withDirectory(previousSource -> {
                    Configuration result =
                            invoke(configOptions(Arrays.asList(
                                    ConfigOption.source(currentSource),
                                    ConfigOption.source(previousSource),
                                    coBucket)));
                    assertThat(result.sources.paths())
                            .containsExactly(
                                    currentSource,
                                    previousSource);
                });
            });
        }
    }

    @Nested
    @DisplayName("config file includes another source")
    public class ConfigLinkedSourceTests implements TemporaryFolder {
        @Test
        @DisplayName("include the linked source")
        public void configIncludeOtherSource() {
            withDirectory(currentSource -> {
                withDirectory(previousSource -> {
                    createFile(currentSource,
                            thorpConfigFileName,
                            Collections.singletonList(
                                    "source = " + previousSource));
                    Configuration result = invoke(configOptions(Arrays.asList(
                            ConfigOption.source(currentSource),
                            coBucket)));
                    assertThat(result.sources.paths())
                            .containsExactly(
                                    currentSource,
                                    previousSource);
                });
            });
        }

        @Test
        @DisplayName("when linked source has config file")
        public void whenSettingsFileInBothSources() {
            withDirectory(currentSource -> {
                withDirectory(previousSource -> {
                    //given
                    createFile(currentSource,
                            thorpConfigFileName,
                            Arrays.asList(
                                    "source = " + previousSource,
                                    "bucket = current-bucket",
                                    "prefix = current-prefix",
                                    "include = current-include",
                                    "exclude = current-exclude"));
                    createFile(previousSource,
                            thorpConfigFileName,
                            Arrays.asList(
                                    "bucket = previous-bucket",
                                    "prefix = previous-prefix",
                                    "include = previous-include",
                                    "exclude = previous-exclude"));
                    //when
                    Configuration result = invoke(configOptions(Arrays.asList(
                            ConfigOption.source(currentSource),
                            coBucket)));
                    //then
                    assertThat(result.sources.paths()).containsExactly(currentSource, previousSource);
                    assertThat(result.bucket.name()).isEqualTo("current-bucket");
                    assertThat(result.prefix.key()).isEqualTo("current-prefix");
                    assertThat(result.filters).anyMatch(filter -> filter.predicate().test("current-include"));
                    assertThat(result.filters).anyMatch(filter -> filter.predicate().test("current-exclude"));
                    assertThat(result.filters).noneMatch(filter -> filter.predicate().test("previous-include"));
                    assertThat(result.filters).noneMatch(filter -> filter.predicate().test("previous-exclude"));
                });
            });
        }
    }
    @Nested
    @DisplayName("linked source links to third source")
    public class LinkedSourceLinkedSourceTests implements TemporaryFolder {
        @Test
        @DisplayName("ignore third source")
        public void ignoreThirdSource() {
            withDirectory(currentSource -> {
                withDirectory(parentSource -> {
                    createFile(currentSource, thorpConfigFileName,
                            Collections.singletonList("source = " + parentSource));
                    withDirectory(grandParentSource -> {
                        createFile(parentSource, thorpConfigFileName,
                                Collections.singletonList("source = " + grandParentSource));
                        //when
                        Configuration result = invoke(configOptions(Arrays.asList(
                                ConfigOption.source(currentSource), coBucket)));
                        //then
                        assertThat(result.sources.paths())
                                .containsExactly(currentSource, parentSource)
                                .doesNotContain(grandParentSource);
                    });
                });
            });
        }
    }

    @Test
    @DisplayName("when batch mode option then batch mode in configuration")
    public void whenBatchMode_thenBatchMode() {
        Configuration result= invoke(configOptions(Arrays.asList(
                ConfigOption.batchMode(),
                coBucket)));
        assertThat(result.batchMode).isTrue();
    }

    public Configuration invoke(ConfigOptions configOptions) {
        try {
            return ConfigurationBuilder.buildConfig(configOptions);
        } catch (IOException | ConfigValidationException e) {
            throw new RuntimeException(e);
        }
    }
}
