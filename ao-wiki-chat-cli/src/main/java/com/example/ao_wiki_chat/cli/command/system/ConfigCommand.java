package com.example.ao_wiki_chat.cli.command.system;

import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiClientException;
import picocli.CommandLine;

import java.time.Duration;
import java.util.Map;

/**
 * Command for managing CLI configuration.
 * Supports setting, getting, listing, and resetting configuration values.
 */
@CommandLine.Command(
        name = "config",
        description = "Manage CLI configuration",
        subcommands = {
                ConfigCommand.SetCommand.class,
                ConfigCommand.GetCommand.class,
                ConfigCommand.ListCommand.class,
                ConfigCommand.ResetCommand.class
        }
)
public class ConfigCommand implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    /**
     * Creates a ConfigManager instance. Can be overridden in tests.
     *
     * @return ConfigManager instance
     */
    ConfigManager createConfigManager() {
        return new ConfigManager();
    }

    @Override
    public void run() {
        // If no subcommand is provided, show usage
        CommandLine.usage(this, System.out);
    }

    /**
     * Subcommand for setting a configuration value.
     */
    @CommandLine.Command(
            name = "set",
            description = "Set a configuration value"
    )
    public static class SetCommand implements Runnable {

        @CommandLine.Parameters(
                index = "0",
                description = "Configuration key (e.g., api.url, api.timeout.connect, output.format)"
        )
        String key;

        @CommandLine.Parameters(
                index = "1",
                description = "Configuration value"
        )
        String value;

        @CommandLine.Spec
        CommandLine.Model.CommandSpec spec;

        private ConfigManager configManager;

        /**
         * Sets the ConfigManager instance. Can be overridden in tests.
         *
         * @param configManager the ConfigManager instance
         */
        void setConfigManager(ConfigManager configManager) {
            this.configManager = configManager;
        }

        ConfigManager getConfigManager() {
            if (configManager == null) {
                configManager = new ConfigManager();
            }
            return configManager;
        }

        @Override
        public void run() {
            try {
                // Validate key and value
                if (key == null || key.isBlank()) {
                    throw new IllegalArgumentException("Configuration key cannot be empty");
                }
                if (value == null || value.isBlank()) {
                    throw new IllegalArgumentException("Configuration value cannot be empty");
                }

                ConfigManager manager = getConfigManager();
                manager.set(key.trim(), value.trim());
                System.out.println("Configuration updated: " + key + " = " + value);

            } catch (IllegalArgumentException | ApiClientException e) {
                System.err.println("Error: " + e.getMessage());
                throw new CommandLine.ParameterException(spec.commandLine(), e.getMessage());
            } catch (Exception e) {
                System.err.println("Unexpected error: " + e.getMessage());
                e.printStackTrace();
                throw new CommandLine.ExecutionException(spec.commandLine(), "Unexpected error: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Subcommand for getting a configuration value.
     */
    @CommandLine.Command(
            name = "get",
            description = "Get a configuration value"
    )
    public static class GetCommand implements Runnable {

        @CommandLine.Parameters(
                index = "0",
                description = "Configuration key (e.g., api.url, api.timeout.connect, output.format)"
        )
        String key;

        @CommandLine.Spec
        CommandLine.Model.CommandSpec spec;

        private ConfigManager configManager;

        /**
         * Sets the ConfigManager instance. Can be overridden in tests.
         *
         * @param configManager the ConfigManager instance
         */
        void setConfigManager(ConfigManager configManager) {
            this.configManager = configManager;
        }

        ConfigManager getConfigManager() {
            if (configManager == null) {
                configManager = new ConfigManager();
            }
            return configManager;
        }

        @Override
        public void run() {
            try {
                // Validate key
                if (key == null || key.isBlank()) {
                    throw new IllegalArgumentException("Configuration key cannot be empty");
                }

                ConfigManager manager = getConfigManager();
                com.example.ao_wiki_chat.cli.config.CliConfig config = manager.get();

                String value = getConfigValue(config, key.trim());
                if (value == null) {
                    throw new IllegalArgumentException("Unknown configuration key: " + key);
                }

                System.out.println(value);

            } catch (IllegalArgumentException e) {
                System.err.println("Error: " + e.getMessage());
                throw new CommandLine.ParameterException(spec.commandLine(), e.getMessage());
            } catch (Exception e) {
                System.err.println("Unexpected error: " + e.getMessage());
                e.printStackTrace();
                throw new CommandLine.ExecutionException(spec.commandLine(), "Unexpected error: " + e.getMessage(), e);
            }
        }

        /**
         * Gets a configuration value by key.
         *
         * @param config the configuration object
         * @param key    the configuration key
         * @return the configuration value as string, or null if key is unknown
         */
        private String getConfigValue(com.example.ao_wiki_chat.cli.config.CliConfig config, String key) {
            return switch (key) {
                case "api.url" -> config.getApiUrl();
                case "api.timeout.connect" -> String.valueOf(config.getApiConnectTimeout().getSeconds());
                case "api.timeout.read" -> String.valueOf(config.getApiReadTimeout().getSeconds());
                case "api.timeout.write" -> String.valueOf(config.getApiWriteTimeout().getSeconds());
                case "output.format" -> config.getOutputFormat();
                case "output.colors" -> String.valueOf(config.isOutputColors());
                default -> null;
            };
        }
    }

    /**
     * Subcommand for listing all configuration values.
     */
    @CommandLine.Command(
            name = "list",
            description = "List all configuration values"
    )
    public static class ListCommand implements Runnable {

        @CommandLine.Spec
        CommandLine.Model.CommandSpec spec;

        private ConfigManager configManager;

        /**
         * Sets the ConfigManager instance. Can be overridden in tests.
         *
         * @param configManager the ConfigManager instance
         */
        void setConfigManager(ConfigManager configManager) {
            this.configManager = configManager;
        }

        ConfigManager getConfigManager() {
            if (configManager == null) {
                configManager = new ConfigManager();
            }
            return configManager;
        }

        @Override
        public void run() {
            try {
                ConfigManager manager = getConfigManager();
                com.example.ao_wiki_chat.cli.config.CliConfig config = manager.get();

                System.out.println("Configuration:");
                System.out.println("  api.url = " + config.getApiUrl());
                System.out.println("  api.timeout.connect = " + config.getApiConnectTimeout().getSeconds());
                System.out.println("  api.timeout.read = " + config.getApiReadTimeout().getSeconds());
                System.out.println("  api.timeout.write = " + config.getApiWriteTimeout().getSeconds());
                System.out.println("  output.format = " + config.getOutputFormat());
                System.out.println("  output.colors = " + config.isOutputColors());

            } catch (Exception e) {
                System.err.println("Unexpected error: " + e.getMessage());
                e.printStackTrace();
                throw new CommandLine.ExecutionException(spec.commandLine(), "Unexpected error: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Subcommand for resetting configuration to defaults.
     */
    @CommandLine.Command(
            name = "reset",
            description = "Reset configuration to default values"
    )
    public static class ResetCommand implements Runnable {

        @CommandLine.Spec
        CommandLine.Model.CommandSpec spec;

        private ConfigManager configManager;

        /**
         * Sets the ConfigManager instance. Can be overridden in tests.
         *
         * @param configManager the ConfigManager instance
         */
        void setConfigManager(ConfigManager configManager) {
            this.configManager = configManager;
        }

        ConfigManager getConfigManager() {
            if (configManager == null) {
                configManager = new ConfigManager();
            }
            return configManager;
        }

        @Override
        public void run() {
            try {
                ConfigManager manager = getConfigManager();
                manager.reset();
                System.out.println("Configuration reset to defaults");

            } catch (Exception e) {
                System.err.println("Unexpected error: " + e.getMessage());
                e.printStackTrace();
                throw new CommandLine.ExecutionException(spec.commandLine(), "Unexpected error: " + e.getMessage(), e);
            }
        }
    }
}
