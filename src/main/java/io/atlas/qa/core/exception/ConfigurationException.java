package io.atlas.qa.core.exception;

/** Raised when a mandatory configuration key is missing or cannot be parsed. */
public class ConfigurationException extends AtlasException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
