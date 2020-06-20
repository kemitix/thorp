package net.kemitix.thorp.domain;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.kemitix.mon.TypeAlias;

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
    class Include extends TypeAlias<Pattern> implements Filter {
        private Include(Pattern value) {
            super(value);
        }
        public static Include create(String include) {
            return new Include(Pattern.compile(include));
        }
        public static Include all() {
            return Include.create(".*");
        }
        @Override
        public Predicate<String> predicate() {
            return getValue().asPredicate();
        }
    }
    class Exclude extends TypeAlias<Pattern> implements Filter {
        private Exclude(Pattern value) {
            super(value);
        }
        public static Exclude create(String exclude) {
            return new Exclude(Pattern.compile(exclude));
        }
        @Override
        public Predicate<String> predicate() {
            return getValue().asPredicate();
        }
    }
}
