package net.kemitix.thorp.config;

import net.kemitix.thorp.domain.Sources;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

public interface SourceConfigLoader {

    static ConfigOptions loadSourceConfigs(Sources sources) throws IOException {
        // add each source as an option
        ConfigOptions configOptions =
                ConfigOptions.create(
                        sources.paths()
                                .stream()
                                .peek(path -> {
                                    System.out.println("Using source: " + path);
                                })
                                .map(ConfigOption::source)
                                .collect(Collectors.toList()));
        // add settings from each source as options
        for (Path path : sources.paths()) {
            configOptions = configOptions.merge(
                    ParseConfigFile.parseFile(
                            new File(path.toFile(), ".thorp.conf")));
        }
        return configOptions;
    }

}
