package net.kemitix.thorp.config;

import java.io.File;
import java.io.IOException;

public interface ConfigurationBuilder {
    static Configuration buildConfig(ConfigOptions priorityOpts) throws IOException, ConfigValidationException {
        return new ConfigurationBuilderImpl().buildConfig(priorityOpts);
    }
    class ConfigurationBuilderImpl implements ConfigurationBuilder {
        private static final String userConfigFile = ".config/thorp.conf";
        private static final File globalConfig = new File("/etc/thorp.conf");
        private static final File userHome = new File(System.getProperty("user.home"));
        Configuration buildConfig(ConfigOptions priorityOpts) throws IOException, ConfigValidationException {
            return ConfigValidator.validateConfig(
                    collateOptions(getConfigOptions(priorityOpts)));
        }
        private ConfigOptions getConfigOptions(ConfigOptions priorityOpts) throws IOException {
            ConfigOptions sourceOpts = SourceConfigLoader.loadSourceConfigs(ConfigQuery.sources(priorityOpts));
            ConfigOptions userOpts = userOptions(priorityOpts.merge(sourceOpts));
            ConfigOptions globalOpts = globalOptions(priorityOpts.merge(sourceOpts.merge(userOpts)));
            return priorityOpts.merge(sourceOpts.merge(userOpts.merge(globalOpts)));
        }
        private ConfigOptions userOptions(ConfigOptions priorityOpts) throws IOException {
            if (ConfigQuery.ignoreUserOptions(priorityOpts)) {
                return ConfigOptions.empty();
            }
            return ParseConfigFile.parseFile(
                    new File(userHome, userConfigFile));
        }
        private ConfigOptions globalOptions(ConfigOptions priorityOpts) throws IOException {
            if (ConfigQuery.ignoreGlobalOptions(priorityOpts)) {
                return ConfigOptions.empty();
            }
            return ParseConfigFile.parseFile(globalConfig);
        }
        private Configuration collateOptions(ConfigOptions configOptions) {
            Configuration config = Configuration.create();
            for (ConfigOption configOption : configOptions.options()) {
                config = configOption.update(config);
            }
            return config;
        }
    }
}
