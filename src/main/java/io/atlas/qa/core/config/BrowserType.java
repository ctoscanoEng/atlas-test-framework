package io.atlas.qa.core.config;

import io.atlas.qa.core.exception.ConfigurationException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Supported browsers.
 * <p>
 * The enum is the single point where a new browser is registered: the options
 * providers are keyed on this type, so adding a browser is an additive change
 * (open/closed principle) rather than an edit of a chain of {@code if/else}.
 */
public enum BrowserType {

    CHROME("chrome"),
    FIREFOX("firefox"),
    EDGE("edge"),
    SAFARI("safari");

    private final String key;

    BrowserType(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static BrowserType from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ConfigurationException("Browser not specified. Valid values: " + supported());
        }
        return Arrays.stream(values())
                .filter(b -> b.key.equalsIgnoreCase(raw.trim()))
                .findFirst()
                .orElseThrow(() -> new ConfigurationException(
                        "Unsupported browser '%s'. Valid values: %s".formatted(raw, supported())));
    }

    private static String supported() {
        return Arrays.stream(values()).map(BrowserType::key).collect(Collectors.joining(", "));
    }
}
