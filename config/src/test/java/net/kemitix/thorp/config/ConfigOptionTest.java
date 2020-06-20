package net.kemitix.thorp.config;

import net.kemitix.thorp.domain.Sources;
import net.kemitix.thorp.filesystem.TemporaryFolder;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class ConfigOptionTest
        implements TemporaryFolder, WithAssertions {
    @Test
    @DisplayName("when more the one source then preserve their order")
    public void whenMultiSource_PreserveOrder() {
        withDirectory(path1 -> {
            withDirectory(path2 -> {
                ConfigOptions configOptions = ConfigOptions.create(
                        Arrays.asList(
                                ConfigOption.source(path1),
                                ConfigOption.source(path2),
                                ConfigOption.bucket("bucket"),
                                ConfigOption.ignoreGlobalOptions(),
                                ConfigOption.ignoreUserOptions()
                        ));
                List<Path> expected = Arrays.asList(path1, path2);
                assertThatCode(() -> {
                    Configuration result =
                            ConfigurationBuilder.buildConfig(configOptions);
                    assertThat(result.sources.paths()).isEqualTo(expected);
                }).doesNotThrowAnyException();
            });
        });
    }
}
