package net.kemitix.thorp.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public interface ParseConfigFile {
    static ConfigOptions parseFile(File file) throws IOException {
        if (file.exists()) {
            System.out.println("Reading config: " + file);
            ConfigOptions configOptions = new ParseConfigLines()
                    .parseLines(Files.readAllLines(file.toPath()));
            return configOptions;
        }
        return ConfigOptions.empty();
    }
}
