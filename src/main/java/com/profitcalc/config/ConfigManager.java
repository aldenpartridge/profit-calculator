package com.profitcalc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ProfitCalc/Config");
    private static final ConfigManager INSTANCE = new ConfigManager();
    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILE = "profit-calc.json";

    private final Gson gson;
    private Config config;
    private File configFile;

    private ConfigManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    public void load() {
        try {
            // Create config directory if it doesn't exist
            Path configPath = Paths.get(CONFIG_DIR);
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
            }

            configFile = new File(CONFIG_DIR, CONFIG_FILE);

            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    config = gson.fromJson(reader, Config.class);
                    LOGGER.info("Loaded configuration from {}", configFile.getPath());
                }
            } else {
                // Create default config
                config = new Config();
                save();
                LOGGER.info("Created default configuration at {}", configFile.getPath());
            }

        } catch (IOException e) {
            LOGGER.error("Error loading config: {}", e.getMessage());
            config = new Config();
        }
    }

    public void save() {
        try {
            if (configFile == null) {
                configFile = new File(CONFIG_DIR, CONFIG_FILE);
            }

            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(config, writer);
                LOGGER.info("Saved configuration to {}", configFile.getPath());
            }

        } catch (IOException e) {
            LOGGER.error("Error saving config: {}", e.getMessage());
        }
    }

    public String getApiKey() {
        return config != null ? config.apiKey : null;
    }

    public void setApiKey(String apiKey) {
        if (config == null) {
            config = new Config();
        }
        config.apiKey = apiKey;
        save();
    }

    public boolean isAutoRefreshEnabled() {
        return config != null && config.autoRefresh;
    }

    public void setAutoRefresh(boolean enabled) {
        if (config == null) {
            config = new Config();
        }
        config.autoRefresh = enabled;
        save();
    }

    public int getRefreshInterval() {
        return config != null ? config.refreshIntervalMinutes : 5;
    }

    public void setRefreshInterval(int minutes) {
        if (config == null) {
            config = new Config();
        }
        config.refreshIntervalMinutes = minutes;
        save();
    }

    private static class Config {
        private String apiKey = "";
        private boolean autoRefresh = true;
        private int refreshIntervalMinutes = 5;
    }
}
