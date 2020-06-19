package net.kemitix.thorp.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public interface ParseConfigFile {
    static ConfigOptions parseFile(File file) throws IOException {
        if (file.exists()) {
            return new ParseConfigLines()
                    .parseLines(Files.readAllLines(file.toPath()));
        }
        return ConfigOptions.empty();
    }
}
