package net.kemitix.thorp.lib;

import net.kemitix.thorp.domain.Filter;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FiltersTest
        implements WithAssertions {

    private final String path1 = "a-file";
    private final String path2 = "another-file.txt";
    private final String path3 = "/path/to/a/file.txt";
    private final String path4 = "/path/to/another/file";
    private final String path5 = "/home/pcampbell/repos/kemitix/s3thorp";
    private final String path6 = "/kemitix/s3thorp/upload/subdir";
    private final List<Path> paths =
            Stream.of(path1, path2, path3, path4, path5, path6)
                    .map(Paths::get)
                    .collect(Collectors.toList());

    @Nested
    @DisplayName("include")
    public class IncludeTests {
        @Test
        @DisplayName("default filter")
        public void defaultFilter() {
            //given
            List<Filter> filters = Collections.singletonList(
                    Filter.Include.all());
            //then
            assertThat(paths)
                    .allMatch(path ->
                            Filters.isIncluded(path, filters));
        }
        @Nested
        @DisplayName("directory exact match")
        public class DirectoryExactMatchTests {
            List<Filter> filters = Collections.singletonList(
                    Filter.include("/upload/subdir/"));
            @Test
            @DisplayName("include matching directory")
            public void includeMatchingDirectory() {
                //given
                Path path = Paths.get("/upload/subdir/leaf-dir");
                //when
                boolean included = Filters.isIncluded(path, filters);
                //then
                assertThat(included).isTrue();
            }
            @Test
            @DisplayName("exclude non-matching file")
            public void excludeNonMatchingFile() {
                //given
                Path path = Paths.get("/upload/other-file");
                //when
                boolean included = Filters.isIncluded(path, filters);
                //then
                assertThat(included).isFalse();
            }
        }
        @Nested
        @DisplayName("file partial match 'root'")
        public class FilePartialMatchRootTests {
            List<Filter> filters = Collections.singletonList(
                    Filter.include("root"));
            @Test
            @DisplayName("include matching file")
            public void includeMatchingFile() {
                //given
                Path path = Paths.get("/upload/root-file");
                //when
                boolean included = Filters.isIncluded(path, filters);
                //then
                assertThat(included).isTrue();
            }
            @Test
            @DisplayName("exclude non-matching file (1)")
            public void excludeNonMatchingFile1() {
                //given
                Path path = Paths.get("/test-file-for-hash.txt");
                //when
                boolean included = Filters.isIncluded(path, filters);
                //then
                assertThat(included).isFalse();
            }
            @Test
            @DisplayName("exclude non-matching file (2)")
            public void excludeNonMatchingFile2() {
                //given
                Path path = Paths.get("/upload/subdir/lead-file");
                //when
                boolean included = Filters.isIncluded(path, filters);
                //then
                assertThat(included).isFalse();
            }
        }
    }
    @Nested
    @DisplayName("exclude")
    public class ExcludeTests {
        @Nested
        @DisplayName("directory exact match exclude")
        public class DirectoryMatchTests {
            List<Filter> filters = Collections.singletonList(
                    Filter.exclude("/upload/subdir/"));
            @Test
            @DisplayName("exclude matching directory")
            public void excludeDirectory() {
                //given
                Path path = Paths.get("/upload/subdir/leaf-file");
                //when
                boolean included = Filters.isIncluded(path, filters);
                //then
                assertThat(included).isFalse();
            }
            @Test
            @DisplayName("include non-matching file")
            public void includeFile() {
                //given
                Path path = Paths.get("/upload/other-file");
                //when
                boolean included = Filters.isIncluded(path, filters);
                //then
                assertThat(included).isTrue();
            }
        }
        @Nested
        @DisplayName("file partial match")
        public class PartialMatchTests {
            List<Filter> filters = Collections.singletonList(
                    Filter.exclude("root"));
            @Test
            @DisplayName("exclude matching file")
            public void excludeFile() {
                //given
                Path path = Paths.get("/upload/root-file");
                //when
                boolean included = Filters.isIncluded(path, filters);
                //then
                assertThat(included).isFalse();
            }
            @Test
            @DisplayName("include non-matching file (1)")
            public void includeFile1() {
                //given
                Path path = Paths.get("/test-file-for-hash.txt");
                //when
                boolean included = Filters.isIncluded(path, filters);
                //then
                assertThat(included).isTrue();
            }
            @Test
            @DisplayName("include non-matching file (2)")
            public void includeFile2() {
                //given
                Path path = Paths.get("/upload/subdir/leaf-file");
                //when
                boolean included = Filters.isIncluded(path, filters);
                //then
                assertThat(included).isTrue();
            }
        }
    }
    @Nested
    @DisplayName("isIncluded")
    public class IsIncludedTests {
        List<Path> invoke(List<Filter> filters) {
            return paths.stream()
                    .filter(path ->
                            Filters.isIncluded(path, filters))
                    .collect(Collectors.toList());
        }

        @Test
        @DisplayName("when no filters then accepts all paths")
        public void whenNoFilters_thenAcceptAll() {
            assertThat(invoke(Collections.emptyList()))
                    .containsExactlyElementsOf(paths);
        }
    }
}
