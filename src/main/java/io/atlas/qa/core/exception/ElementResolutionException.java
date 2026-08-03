package io.atlas.qa.core.exception;

import io.atlas.qa.core.element.Locator;

import java.time.Duration;

/**
 * Raised when none of the strategies declared by a {@link Locator} matched an
 * element within the allotted time.
 * <p>
 * The message is intentionally verbose: when a suite of 400 tests goes red at
 * 3 a.m. in a pipeline, the exception text is the only artefact the on-call
 * engineer reads first.
 */
public class ElementResolutionException extends AtlasException {

    public ElementResolutionException(Locator locator, Duration waited) {
        super("""
              Unable to resolve element [%s] after %d ms.
              Strategies attempted (in order): %s
              Hint: the element may be inside an iframe or a shadow root, the page may not \
              have finished loading, or the application markup has changed.\
              """.formatted(locator.description(), waited.toMillis(), locator.strategiesAsText()));
    }
}
