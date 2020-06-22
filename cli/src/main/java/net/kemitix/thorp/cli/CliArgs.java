package net.kemitix.thorp.cli;

import net.kemitix.thorp.config.ConfigOption;
import net.kemitix.thorp.config.ConfigOptions;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CliArgs {
    @Override
    public String toString() {
        return "CliArgs{" +
                "showVersion=" + showVersion +
                ",\n batchMode=" + batchMode +
                ",\n sources=" + sources +
                ",\n bucket='" + bucket + '\'' +
                ",\n prefix='" + prefix + '\'' +
                ",\n parallel=" + parallel +
                ",\n includes=" + includes +
                ",\n excludes=" + excludes +
                ",\n debug=" + debug +
                ",\n ignoreUserOptions=" + ignoreUserOptions +
                ",\n ignoreGlobalOptions=" + ignoreGlobalOptions +
                "\n}";
    }

    public static ConfigOptions parse(String[] args) {
        CliArgs cliArgs = new CliArgs();
        new CommandLine(cliArgs).parseArgs(args);
        return ConfigOptions.empty()
                .merge(flag(cliArgs.showVersion, ConfigOption::version))
                .merge(flag(cliArgs.debug, ConfigOption::debug))
                .merge(flag(cliArgs.batchMode, ConfigOption::batchMode))
                .merge(flag(cliArgs.ignoreGlobalOptions, ConfigOption::ignoreGlobalOptions))
                .merge(flag(cliArgs.ignoreUserOptions, ConfigOption::ignoreUserOptions))
                .merge(option(cliArgs.bucket, ConfigOption::bucket))
                .merge(option(cliArgs.prefix, ConfigOption::prefix))
                .merge(paths(cliArgs.sources, ConfigOption::source))
                .merge(strings(cliArgs.includes, ConfigOption::include))
                .merge(strings(cliArgs.excludes, ConfigOption::exclude))
                .merge(integer(cliArgs.parallel, 1, ConfigOption::parallel));
    }

    private static ConfigOptions flag(
            boolean flag,
            Supplier<ConfigOption> configOption
    ) {
        if (flag)
            return ConfigOptions.create(Collections.singletonList(
                    configOption.get()));
        return ConfigOptions.empty();
    }

    private static ConfigOptions option(
            String value,
            Function<String, ConfigOption> configOption
    ) {
        if (value.isEmpty())
            return ConfigOptions.empty();
        return ConfigOptions.create(Collections.singletonList(
                configOption.apply(value)));
    }

    private static ConfigOptions integer(
            int value,
            int defaultValue,
            Function<Integer, ConfigOption> configOption
    ) {
        if (value == defaultValue) return ConfigOptions.empty();
        return ConfigOptions.create(Collections.singletonList(
                configOption.apply(value)));
    }

    private static ConfigOptions paths(
            List<String> values,
            Function<Path, ConfigOption> configOption
    ) {
        if (values == null) return ConfigOptions.empty();
        return ConfigOptions.create(
                values.stream()
                        .map(Paths::get)
                        .map(configOption)
                        .collect(Collectors.toList()));
    }

    private static ConfigOptions strings(
            List<String> values,
            Function<String, ConfigOption> configOption
    ) {
        if (values == null) return ConfigOptions.empty();
        return ConfigOptions.create(
                values.stream()
                        .map(configOption)
                        .collect(Collectors.toList()));
    }

    @Option(
            names = {"-V", "--version"},
            description = "Show version")
    boolean showVersion;

    @Option(
            names = {"-B", "--batch"},
            description = "Enable batch-mode")
    boolean batchMode;

    @Option(
            names = {"-s", "--source"},
            description = "Source directory to sync to destination")
    List<String> sources;

    @Option(
            names = {"-b", "--bucket"},
            defaultValue = "",
            description = "S3 bucket name")
    String bucket;

    @Option(
            names = {"-p", "--prefix"},
            defaultValue = "",
            description = "Prefix within the S3 Bucket")
    String prefix;

    @Option(
            names = {"-P", "--parallel"},
            defaultValue = "1",
            description = "Maximum Parallel uploads")
    int parallel;

    @Option(
            names = {"-i", "--include"},
            description = "Include only matching paths")
    List<String> includes;

    @Option(
            names = {"-x", "--exclude"},
            description = "Exclude matching paths")
    List<String> excludes;

    @Option(
            names = {"-d", "--debug"},
            description = "Enable debug logging")
    boolean debug;

    @Option(
            names = {"--no-user"},
            description = "Ignore user configuration")
    boolean ignoreUserOptions;

    @Option(
            names = {"--no-global"},
            description = "Ignore global configuration")
    boolean ignoreGlobalOptions;

}
