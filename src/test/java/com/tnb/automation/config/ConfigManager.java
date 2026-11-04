package com.tnb.automation.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigManager {

    private static final Properties PROPS = new Properties();

    static {
        String env = System.getProperty("env", "dev");     // mvn test -Denv=sit
        String file = "config/" + env + ".properties";
        try (InputStream in = ConfigManager.class.getClassLoader().getResourceAsStream(file)) {
            if (in == null) {
                throw new IllegalStateException("Config file not found: " + file);
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read config file: " + file, e);
        }
    }

    private ConfigManager() {
    }

    public static String get(String key) {
        String value = PROPS.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing config key: " + key);
        }
        return value;
    }
}