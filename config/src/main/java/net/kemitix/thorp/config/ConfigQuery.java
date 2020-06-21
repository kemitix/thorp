package net.kemitix.thorp.config;

import net.kemitix.thorp.domain.Sources;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface ConfigQuery {

    static boolean showVersion(ConfigOptions configOptions) {
        return configOptions.options().stream()
                .anyMatch(configOption ->
                        configOption instanceof ConfigOption.Version);
    }

    static boolean batchMode(ConfigOptions configOptions) {
        return configOptions.options().stream()
                .anyMatch(configOption ->
                        configOption instanceof ConfigOption.BatchMode);
    }

    static boolean ignoreUserOptions(ConfigOptions configOptions) {
        return configOptions.options().stream()
                .anyMatch(configOption ->
                        configOption instanceof ConfigOption.IgnoreUserOptions);
    }

    static boolean ignoreGlobalOptions(ConfigOptions configOptions) {
        return configOptions.options().stream()
                .anyMatch(configOption ->
                        configOption instanceof ConfigOption.IgnoreGlobalOptions);
    }

    static Sources sources(ConfigOptions configOptions) {
        List<Path> explicitPaths = configOptions.options().stream()
                .filter(configOption ->
                        configOption instanceof ConfigOption.Source)
                .map(ConfigOption.Source.class::cast)
                .map(ConfigOption.Source::path)
                .collect(Collectors.toList());
        if (explicitPaths.isEmpty()) {
            return Sources.create(Collections.singletonList(Paths.get(System.getenv("PWD"))));
        }
        return Sources.create(explicitPaths);
    }
}
