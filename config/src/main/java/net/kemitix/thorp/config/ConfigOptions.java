package net.kemitix.thorp.config;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
                .map(p -> p.factor)
                .orElse(1);
    }

    static ConfigOptions empty() {
        return create(Collections.emptyList());
    }

    static ConfigOptions create(List<ConfigOption> options) {
        return new ConfigOptionsImpl(options);
    }
    @RequiredArgsConstructor
    class ConfigOptionsImpl implements ConfigOptions {
        private final List<ConfigOption> options;
        @Override
        public List<ConfigOption> options() {
            return new ArrayList<>(options);
        }
        @Override
        public ConfigOptions merge(ConfigOptions other) {
            List<ConfigOption> options = options();
            options.addAll(other.options());
            return ConfigOptions.create(options);
        }
        @Override
        public ConfigOptions prepend(ConfigOption configOption) {
            List<ConfigOption> options = new ArrayList<>();
            options.add(configOption);
            options.addAll(options());
            return ConfigOptions.create(options);
        }

        @Override
        public boolean containsInstanceOf(Class<? extends ConfigOption> type) {
            return options.stream()
                    .anyMatch(option ->
                            type.isAssignableFrom(option.getClass()));
        }
    }
}
