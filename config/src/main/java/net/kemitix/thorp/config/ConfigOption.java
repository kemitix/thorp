package net.kemitix.thorp.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.kemitix.thorp.domain.Bucket;
import net.kemitix.thorp.domain.Filter;
import net.kemitix.thorp.domain.RemoteKey;
import net.kemitix.thorp.domain.Sources;
import zio.ZIO;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public interface ConfigOption {
    Configuration update(Configuration config);

    static ConfigOption source(Path path) {
        return new Source(path);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Source implements ConfigOption {
        public final Path path;
        @Override
        public Configuration update(Configuration config) {
            return config.withSources(config.sources.append(path));
        }
    }

    static ConfigOption bucket(String name) {
        return new Bucket(name);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Bucket implements ConfigOption {
        public final String name;
        @Override
        public Configuration update(Configuration config) {
            return config.withBucket(
                    net.kemitix.thorp.domain.Bucket.named(name));

        }
    }

    static ConfigOption prefix(String path) {
        return new Prefix(path);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Prefix implements ConfigOption {
        public final String path;
        @Override
        public Configuration update(Configuration config) {
            return config.withPrefix(RemoteKey.create(path));
        }
    }

    static ConfigOption include(String pattern) {
        return new Include(pattern);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Include implements ConfigOption {
        public final String pattern;
        @Override
        public Configuration update(Configuration config) {
            List<Filter> filters = new ArrayList<>(config.filters);
            filters.add(net.kemitix.thorp.domain.Filter.include(pattern));
            return config.withFilters(filters);
        }
    }

    static ConfigOption exclude(String pattern) {
        return new Exclude(pattern);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Exclude implements ConfigOption {
        public final String pattern;
        @Override
        public Configuration update(Configuration config) {
            List<Filter> filters = new ArrayList<>(config.filters);
            filters.add(net.kemitix.thorp.domain.Filter.exclude(pattern));
            return config.withFilters(filters);
        }
    }

    static ConfigOption debug() {
        return new Debug();
    }
    class Debug implements ConfigOption {
        @Override
        public Configuration update(Configuration config) {
            return config.withDebug(true);
        }
    }

    static ConfigOption batchMode() {
        return new BatchMode();
    }
    class BatchMode implements ConfigOption {
        @Override
        public Configuration update(Configuration config) {
            return config.withDebug(true);
        }
    }

    static ConfigOption version() {
        return new Version();
    }
    class Version implements ConfigOption {
        @Override
        public Configuration update(Configuration config) {
            return config;
        }
    }

    static ConfigOption ignoreUserOptions() {
        return new IgnoreUserOptions();
    }
    class IgnoreUserOptions implements ConfigOption {
        @Override
        public Configuration update(Configuration config) {
            return config;
        }
    }

    static ConfigOption ignoreGlobalOptions() {
        return new IgnoreGlobalOptions();
    }
    class IgnoreGlobalOptions implements ConfigOption {
        @Override
        public Configuration update(Configuration config) {
            return config;
        }
    }

    static ConfigOption parallel(int factor) {
        return new Parallel(factor);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Parallel implements ConfigOption {
        public final int factor;
        @Override
        public Configuration update(Configuration config) {
            return config.withParallel(factor);

        }
    }
}
