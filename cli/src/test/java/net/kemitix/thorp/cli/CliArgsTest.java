package net.kemitix.thorp.cli;

import net.kemitix.thorp.config.ConfigOption;
import net.kemitix.thorp.config.ConfigOptions;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CliArgsTest
        implements WithAssertions {
    @Test
    @DisplayName("when no args then empty options")
    public void whenNoArgs_thenEmptyOptions() {
        assertThat(invoke(new String[]{}))
                .isEqualTo(ConfigOptions.empty());
    }

    @Test
    @DisplayName("version")
    public void version() {
        testOptions(new String[]{"-V", "--version"},
                ConfigOption::version);
    }

    @Test
    @DisplayName("batch")
    public void batch() {
        testOptions(new String[]{"-B", "--batch"},
                ConfigOption::batchMode);
    }

    @Test
    @DisplayName("debug")
    public void debug() {
        testOptions(new String[]{"-d", "--debug"},
                ConfigOption::debug);
    }

    @Test
    @DisplayName("ignore user options")
    public void ignoreUserOptions() {
        testOptions(new String[]{"--no-user"},
                ConfigOption::ignoreUserOptions);
    }

    @Test
    @DisplayName("ignore global options")
    public void ignoreGlobalOptions() {
        testOptions(new String[]{"--no-global"},
                ConfigOption::ignoreGlobalOptions);
    }

    @Test
    @DisplayName("bucket")
    public void bucket() {
        testStringOptions(new String[]{"--bucket"},
                ConfigOption::bucket);
    }

    @Test
    @DisplayName("prefix")
    public void prefix() {
        testStringOptions(new String[]{"--prefix"},
                ConfigOption::prefix);
    }

    @Test
    @DisplayName("source")
    public void source() {
        testPathsOptions(
                new String[]{"--source"},
                ConfigOption::source);
    }

    @Test
    @DisplayName("include")
    public void include() {
        testStringListOptions(
                new String[]{"--include"},
                ConfigOption::include);
    }

    @Test
    @DisplayName("exclude")
    public void exclude() {
        testStringListOptions(
                new String[]{"--exclude"},
                ConfigOption::exclude);
    }

    @Test
    @DisplayName("parallel")
    public void parallel() {
        testIntOptions(
                new String[]{"--parallel"},
                ConfigOption::parallel);
    }

    private void testOptions(
            String[] parameters,
            Supplier<ConfigOption> optionSupplier
    ) {
        assertThat(Arrays.asList(parameters))
                .allSatisfy(arg -> assertThat(
                        invoke(new String[]{arg}))
                        .isEqualTo(
                                ConfigOptions.create(
                                        Collections.singletonList(
                                                optionSupplier.get()
                                        ))));
    }

    private void testStringOptions(
            String[] parameters,
            Function<String, ConfigOption> valueToOption
    ) {
        String value = UUID.randomUUID().toString();
        assertThat(Arrays.asList(parameters))
                .allSatisfy(arg -> assertThat(
                        invoke(new String[]{arg, value}))
                        .isEqualTo(
                                ConfigOptions.create(
                                        Collections.singletonList(
                                                valueToOption.apply(value)
                                        ))));
    }

    private void testIntOptions(
            String[] parameters,
            Function<Integer, ConfigOption> valueToOption
    ) {
        Integer value = new Random().nextInt();
        assertThat(Arrays.asList(parameters))
                .allSatisfy(arg -> assertThat(
                        invoke(new String[]{arg, "" + value}))
                        .isEqualTo(
                                ConfigOptions.create(
                                        Collections.singletonList(
                                                valueToOption.apply(value)
                                        ))));
    }

    private void testPathsOptions(
            String[] parameters,
            Function<Path, ConfigOption> pathToOption
    ) {
        String value1 = UUID.randomUUID().toString();
        String value2 = UUID.randomUUID().toString();
        List<ConfigOption> paths = Stream.of(value1, value2)
                .map(Paths::get)
                .map(pathToOption)
                .collect(Collectors.toList());
        assertThat(Arrays.asList(parameters))
                .allSatisfy(arg -> assertThat(
                        invoke(new String[]{
                                arg, value1,
                                arg, value2}))
                        .isEqualTo(
                                ConfigOptions.create(
                                                paths
                                        )));
    }

    private void testStringListOptions(
            String[] parameters,
            Function<String, ConfigOption> stringToOption
    ) {
        String value1 = UUID.randomUUID().toString();
        String value2 = UUID.randomUUID().toString();
        List<ConfigOption> expected = Stream.of(value1, value2)
                .map(stringToOption)
                .collect(Collectors.toList());
        assertThat(Arrays.asList(parameters))
                .allSatisfy(arg -> assertThat(
                        invoke(new String[]{arg, value1, arg, value2}))
                        .isEqualTo(ConfigOptions.create(expected)));
    }

    ConfigOptions invoke(String[] args) {
        return CliArgs.parse(args);
    }
}
