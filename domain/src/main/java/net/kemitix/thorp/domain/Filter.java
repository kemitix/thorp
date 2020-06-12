package net.kemitix.thorp.domain;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public interface Filter {
    static Include include(String include) {
        return Include.create(include);
    }
    static Exclude exclude(String exclude) {
        return Exclude.create(exclude);
    }
    Predicate<String> predicate();
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Include implements Filter {
        private final Pattern include;
        public static Include create(String include) {
            return new Include(Pattern.compile(include));
        }
        public static Include all() {
            return Include.create(".*");
        }
        @Override
        public Predicate<String> predicate() {
            return include.asPredicate();
        }
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Exclude implements Filter {
        private final Pattern exclude;
        public static Exclude create(String exclude) {
            return new Exclude(Pattern.compile(exclude));
        }
        @Override
        public Predicate<String> predicate() {
            return exclude.asPredicate();
        }
    }
}
