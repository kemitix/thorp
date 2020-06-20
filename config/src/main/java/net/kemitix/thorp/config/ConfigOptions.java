package net.kemitix.thorp.config;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.*;

public interface ConfigOptions {
    List<ConfigOption> options();
    ConfigOptions merge(ConfigOptions other);
    ConfigOptions prepend(ConfigOption configOption);
    boolean containsInstanceOf(Class<? extends ConfigOption> type);
    static int parallel(ConfigOptions configOptions) {
        return configOptions.options()
                .stream()
                .filter(option -> option instanceof ConfigOption.Parallel)
                .map(ConfigOption.Parallel.class::cast)
                .findFirst()
                .map(ConfigOption.Parallel::factor)
                .orElse(1);
    }
    static ConfigOptions empty() {
        return create(Collections.emptyList());
    }
    static ConfigOptions create(List<ConfigOption> options) {
        return new ConfigOptionsImpl(options);
    }
    @EqualsAndHashCode
    @RequiredArgsConstructor
    class ConfigOptionsImpl implements ConfigOptions {
        private final List<ConfigOption> options;
        @Override
        public List<ConfigOption> options() {
            return new ArrayList<>(options);
        }
        @Override
        public ConfigOptions merge(ConfigOptions other) {
            List<ConfigOption> optionList = options();
            other.options().stream()
                    .filter(o -> !optionList.contains(o))
                    .forEach(optionList::add);
            return ConfigOptions.create(optionList);
        }
        @Override
        public ConfigOptions prepend(ConfigOption configOption) {
            List<ConfigOption> optionList = new ArrayList<>();
            optionList.add(configOption);
            options().stream()
                    .filter(o -> !optionList.contains(0))
                    .forEach(optionList::add);
            return ConfigOptions.create(optionList);
        }
        @Override
        public boolean containsInstanceOf(Class<? extends ConfigOption> type) {
            return options.stream()
                    .anyMatch(option ->
                            type.isAssignableFrom(option.getClass()));
        }
    }
}
