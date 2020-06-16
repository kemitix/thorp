package net.kemitix.thorp.config;

import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParseConfigLines {
    private static final String pattern = "^\\s*(?<key>\\S*)\\s*=\\s*(?<value>\\S*)\\s*$";
    private static final Pattern format = Pattern.compile(pattern);

    ConfigOptions parseLines(List<String> lines) {
        return ConfigOptions.apply(
                lines.stream()
                        .flatMap(this::parseLine)
                        .collect(Collectors.toList()));
    }

    private Stream<ConfigOption> parseLine(String str) {
        Matcher m = format.matcher(str);
        if (m.matches()) {
            return parseKeyValue(m.group("key"), m.group("value"));
        }
        return Stream.empty();
    }

    private Stream<ConfigOption> parseKeyValue(String key, String value) {
        switch (key.toLowerCase()) {
            case "parallel":
                return Stream.of(new ConfigOption.Parallel(Integer.parseInt(value)));
            case "source":
                return Stream.of(new ConfigOption.Source(Paths.get(value)));
            case "bucket":
                return Stream.of(new ConfigOption.Bucket(value));
            case "prefix":
                return Stream.of(new ConfigOption.Prefix(value));
            case "include":
                return Stream.of(new ConfigOption.Include(value));
            case "exclude":
                return Stream.of(new ConfigOption.Exclude(value));
            case "debug":
                if (truthy(value))
                    return Stream.of(new ConfigOption.Debug());
                // fall through to default
            default:
                return Stream.empty();
        }
    }

    private boolean truthy(String value) {
        switch (value.toLowerCase()) {
            case "true":
            case "yes":
            case "enabled":
                return true;
            default:
                return false;
        }
    }
}
