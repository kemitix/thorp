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
                ConfigOptions.apply(
                        sources.paths()
                                .stream()
                                .map(ConfigOption.Source::new)
                                .map(ConfigOption.class::cast)
                                .collect(Collectors.toList()));
        // add settings from each source as options
        for (Path path : sources.paths()) {
            configOptions = configOptions.$plus$plus(
                    ParseConfigFile.parseFile(
                            new File(path.toFile(), ".thorp.conf")));
        }
        return configOptions;
    }

}
