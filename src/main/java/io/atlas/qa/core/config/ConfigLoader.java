package io.atlas.qa.core.config;

import io.atlas.qa.core.exception.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Resolves the effective configuration by merging four sources, from the
 * weakest to the strongest:
 *
 * <pre>
 *   1. config/atlas.properties            defaults committed to the repository
 *   2. config/atlas-&lt;env&gt;.properties       per-environment overlay
 *   3. environment variables ATLAS_*      how CI/CD and Docker inject values
 *   4. system properties -Datlas.*        how a developer overrides one run
 * </pre>
 *
 * Nothing secret ever lives in the repository: secrets are expected to arrive
 * through (3), which is exactly what Jenkins credentials and GitHub Actions
 * secrets provide.
 * <p>
 * The resolved map is computed once, is immutable and therefore safe to read
 * from every parallel test thread without synchronisation.
 */
public final class ConfigLoader {

    private static final Logger LOG = LogManager.getLogger(ConfigLoader.class);

    private static final String DEFAULTS_FILE = "config/atlas.properties";
    private static final String ENV_FILE_PATTERN = "config/atlas-%s.properties";
    private static final String SYS_PREFIX = "atlas.";
    private static final String ENV_PREFIX = "ATLAS_";
    private static final String ENV_KEY = "env";

    private static final Map<String, String> RESOLVED = resolve();

    private ConfigLoader() {
    }

    /** @return the raw value for {@code key}, or {@code null} when absent. */
    public static String find(String key) {
        return RESOLVED.get(key);
    }

    /** @throws ConfigurationException when the key is not defined anywhere. */
    public static String require(String key) {
        String value = RESOLVED.get(key);
        if (value == null || value.isBlank()) {
            throw new ConfigurationException(
                    "Missing configuration key '%s'. Define it in %s, in the environment overlay, "
                            .formatted(key, DEFAULTS_FILE)
                            + "as ATLAS_%s or as -D%s%s".formatted(toEnvName(key), SYS_PREFIX, key));
        }
        return value;
    }

    public static String get(String key, String fallback) {
        String value = RESOLVED.get(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    public static int getInt(String key, int fallback) {
        String value = get(key, null);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Key '%s' must be an integer but was '%s'".formatted(key, value), e);
        }
    }

    public static boolean getBoolean(String key, boolean fallback) {
        String value = get(key, null);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    /** Immutable snapshot, used by the reporter to document how a run was configured. */
    public static Map<String, String> snapshot() {
        return new TreeMap<>(RESOLVED);
    }

    // ------------------------------------------------------------------ internals

    private static Map<String, String> resolve() {
        Map<String, String> merged = new LinkedHashMap<>();

        Properties defaults = readClasspath(DEFAULTS_FILE, true);
        defaults.forEach((k, v) -> merged.put(String.valueOf(k), String.valueOf(v)));

        String environment = firstNonBlank(
                System.getProperty(SYS_PREFIX + ENV_KEY),
                System.getenv(ENV_PREFIX + "ENV"),
                merged.get(ENV_KEY),
                TargetEnvironment.SANDBOX.key());
        merged.put(ENV_KEY, environment);

        Properties overlay = readClasspath(ENV_FILE_PATTERN.formatted(environment), false);
        overlay.forEach((k, v) -> merged.put(String.valueOf(k), String.valueOf(v)));

        // Environment variables win over files.
        for (String key : merged.keySet().toArray(String[]::new)) {
            String fromEnv = System.getenv(ENV_PREFIX + toEnvName(key));
            if (fromEnv != null && !fromEnv.isBlank()) {
                merged.put(key, fromEnv);
            }
        }

        // System properties win over everything: -Datlas.browser=firefox
        System.getProperties().stringPropertyNames().stream()
                .filter(name -> name.startsWith(SYS_PREFIX))
                .forEach(name -> {
                    String value = System.getProperty(name);
                    if (value != null && !value.isBlank()) {
                        merged.put(name.substring(SYS_PREFIX.length()), value);
                    }
                });

        LOG.info("Configuration resolved for environment '{}' ({} keys)", merged.get(ENV_KEY), merged.size());
        return Collections.unmodifiableMap(merged);
    }

    private static Properties readClasspath(String resource, boolean mandatory) {
        Properties properties = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                if (mandatory) {
                    throw new ConfigurationException("Mandatory configuration file not found on classpath: " + resource);
                }
                LOG.debug("Optional configuration file not present, skipped: {}", resource);
                return properties;
            }
            properties.load(in);
            LOG.debug("Loaded {} keys from {}", properties.size(), resource);
            return properties;
        } catch (IOException e) {
            throw new ConfigurationException("Unable to read configuration file " + resource, e);
        }
    }

    /** {@code timeout.explicit} and {@code baseUrl} both become {@code TIMEOUT_EXPLICIT} / {@code BASE_URL}. */
    private static String toEnvName(String key) {
        return key.replace('.', '_')
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toUpperCase();
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return null;
    }
}
