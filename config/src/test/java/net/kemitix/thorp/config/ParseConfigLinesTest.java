package net.kemitix.thorp.config;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class ParseConfigLinesTest
        implements WithAssertions {

    private final ParseConfigLines parser = new ParseConfigLines();

    @Test
    @DisplayName("source")
    public void source() {
        testParser("source = /path/to/source",
                ConfigOption.source(Paths.get("/path/to/source")));
    }
    @Test
    @DisplayName("bucket")
    public void bucket() {
        testParser("bucket = bucket-name",
                ConfigOption.bucket("bucket-name"));
    }
    @Test
    @DisplayName("prefix")
    public void prefix() {
        testParser("prefix = prefix/to/files",
                ConfigOption.prefix("prefix/to/files"));
    }
    @Test
    @DisplayName("include")
    public void include() {
        testParser("include = path/to/include",
                ConfigOption.include("path/to/include"));
    }
    @Test
    @DisplayName("exclude")
    public void exclude() {
        testParser("exclude = path/to/exclude",
                ConfigOption.exclude("path/to/exclude"));
    }
    @Test
    @DisplayName("parallel")
    public void parallel() {
        testParser("parallel = 3",
                ConfigOption.parallel(3));
    }
    @Test
    @DisplayName("parallel - invalid")
    public void parallelInvalid() {
        testParserIgnores("parallel = invalid");
    }
    @Test
    @DisplayName("debug - true")
    public void debugTrue() {
        testParser("debug = true",
                ConfigOption.debug());
    }
    @Test
    @DisplayName("debug - false")
    public void debugFalse() {
        testParserIgnores("debug = false");
    }
    @Test
    @DisplayName("comment")
    public void comment() {
        testParserIgnores("# ignore name");
    }
    @Test
    @DisplayName("unrecognised option")
    public void unrecognised() {
        testParserIgnores("unsupported = option");
    }

    public void testParser(String line, ConfigOption configOption) {
        assertThat(invoke(Collections.singletonList(line))).isEqualTo(
                ConfigOptions.create(
                        Collections.singletonList(configOption)));
    }

    public void testParserIgnores(String line) {
        assertThat(invoke(Collections.singletonList(line))).isEqualTo(
                ConfigOptions.create(
                        Collections.emptyList()));
    }

    private ConfigOptions invoke(List<String> lines) {
        return parser.parseLines(lines);
    }
}
