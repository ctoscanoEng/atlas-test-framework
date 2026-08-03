package io.atlas.qa.core.exception;

/**
 * Base unchecked exception for every failure originated inside the framework.
 * <p>
 * Rationale: a test must be able to distinguish a <em>framework</em> failure
 * (bad configuration, driver not available, locator not resolvable) from an
 * <em>application</em> failure (a genuine assertion error). Reporting and the
 * retry policy behave differently for the two categories.
 */
public class AtlasException extends RuntimeException {

    public AtlasException(String message) {
        super(message);
    }

    public AtlasException(String message, Throwable cause) {
        super(message, cause);
    }
}
