package io.atlas.qa.core.config;

import io.atlas.qa.core.exception.ConfigurationException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The environment the suite is pointed at.
 * <p>
 * {@link #SANDBOX} is special: it is served by the framework itself
 * ({@code SandboxServer}), which makes the whole suite runnable offline and
 * fully deterministic — no third-party demo site can turn the pipeline red.
 */
public enum TargetEnvironment {

    /** Self-hosted application under test, started in-process. */
    SANDBOX("sandbox"),
    /** Public demo applications reached over the network. */
    STAGING("staging"),
    /** Reserved for a real deployment; credentials must come from the environment. */
    PRODUCTION("production");

    private final String key;

    TargetEnvironment(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static TargetEnvironment from(String raw) {
        return Arrays.stream(values())
                .filter(e -> e.key.equalsIgnoreCase(String.valueOf(raw).trim()))
                .findFirst()
                .orElseThrow(() -> new ConfigurationException(
                        "Unknown environment '%s'. Valid values: %s".formatted(raw,
                                Arrays.stream(values()).map(TargetEnvironment::key)
                                        .collect(Collectors.joining(", ")))));
    }
}
