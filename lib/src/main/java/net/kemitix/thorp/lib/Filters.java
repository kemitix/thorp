package net.kemitix.thorp.lib;

import net.kemitix.thorp.config.Configuration;
import net.kemitix.thorp.domain.Filter;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public interface Filters {

    static boolean isIncluded(
            Configuration configuration,
            File file
    ) {
        return isIncluded(file.toPath(), configuration.filters);
    }

    static boolean isIncluded(
            Path path,
            List<Filter> filters
    ) {
        AtomicBoolean included = new AtomicBoolean(isIncludedByDefault(filters));
        filters.forEach(
                filter -> {
                    if (included.get()) {
                        if (filter instanceof Filter.Exclude) {
                            boolean excludedByFilter = isExcludedByFilter(path, filter);
                            included.set(!excludedByFilter);
                        }
                    } else {
                        if (filter instanceof Filter.Include) {
                            boolean includedByFilter = isIncludedByFilter(path, filter);
                            included.set(includedByFilter);
                        }
                    }
                }
        );
        return included.get();
    }

    static boolean isIncludedByDefault(List<Filter> filters) {
        return filters.isEmpty() ||
                filters.stream()
                        .allMatch(filter ->
                                filter instanceof Filter.Exclude);
    }

    static boolean isIncludedByFilter(Path path, Filter filter) {
        return  filter.predicate().test(path.toFile().getPath());
    }
    static boolean isExcludedByFilter(Path path, Filter filter) {
        return  filter.predicate().test(path.toFile().getPath());
    }
}
