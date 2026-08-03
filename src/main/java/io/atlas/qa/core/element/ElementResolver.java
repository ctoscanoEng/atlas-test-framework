package io.atlas.qa.core.element;

import io.atlas.qa.core.config.AtlasConfig;
import io.atlas.qa.core.exception.ElementResolutionException;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Turns a {@link Locator} into a live {@link WebElement}.
 *
 * <h2>Resolution algorithm</h2>
 * Instead of waiting the full timeout on the primary strategy and only then
 * trying the fallbacks — which would multiply the cost of a broken locator by
 * the number of alternatives — the resolver polls <em>all</em> strategies in
 * round-robin within a single deadline:
 *
 * <pre>
 *   while (now &lt; deadline)
 *       for each strategy s          &#47;&#47; primary first
 *           if findElements(s) is not empty -&gt; return (and log a healing event
 *                                                       when s is not the primary)
 *       sleep(pollingInterval)
 * </pre>
 *
 * A healed lookup therefore costs one extra {@code findElements} round-trip,
 * roughly 10 ms, instead of a full 15-second timeout. Cheap enough to be always
 * on, which is the only way a resilience feature survives contact with a real
 * pipeline.
 */
public final class ElementResolver {

    private final WebDriver driver;
    private final Duration defaultTimeout;

    public ElementResolver(WebDriver driver) {
        this(driver, AtlasConfig.explicitTimeout());
    }

    public ElementResolver(WebDriver driver, Duration defaultTimeout) {
        this.driver = driver;
        this.defaultTimeout = defaultTimeout;
    }

    /** Waits for the element to be present in the DOM. */
    public WebElement resolve(Locator locator) {
        return resolve(locator, defaultTimeout, Visibility.ANY);
    }

    /** Waits for the element to be present <em>and</em> rendered. */
    public WebElement resolveVisible(Locator locator) {
        return resolve(locator, defaultTimeout, Visibility.VISIBLE);
    }

    public WebElement resolve(Locator locator, Duration timeout, Visibility visibility) {
        return tryResolve(locator, timeout, visibility)
                .orElseThrow(() -> new ElementResolutionException(locator, timeout));
    }

    /** Non-throwing variant, for assertions on absence and for optional UI. */
    public Optional<WebElement> tryResolve(Locator locator, Duration timeout, Visibility visibility) {
        List<By> strategies = candidates(locator);
        Instant deadline = Instant.now().plus(timeout);

        do {
            for (int index = 0; index < strategies.size(); index++) {
                Optional<WebElement> found = firstMatch(driver, strategies.get(index), visibility);
                if (found.isPresent()) {
                    if (index > 0) {
                        HealingLedger.record(new HealingLedger.HealingEvent(
                                locator.description(),
                                locator.primary().toString(),
                                strategies.get(index).toString(),
                                index,
                                Instant.now()));
                    }
                    return found;
                }
            }
            sleep(AtlasConfig.pollingInterval());
        } while (Instant.now().isBefore(deadline));

        return Optional.empty();
    }

    /** All elements matching the first strategy that matches anything. */
    public List<WebElement> resolveAll(Locator locator) {
        Instant deadline = Instant.now().plus(defaultTimeout);
        do {
            for (By strategy : candidates(locator)) {
                List<WebElement> elements = driver.findElements(strategy);
                if (!elements.isEmpty()) {
                    return elements;
                }
            }
            sleep(AtlasConfig.pollingInterval());
        } while (Instant.now().isBefore(deadline));
        return List.of();
    }

    /** Resolution scoped to a parent element — the building block of component objects. */
    public Optional<WebElement> resolveWithin(SearchContext parent, Locator locator) {
        for (By strategy : candidates(locator)) {
            List<WebElement> elements = parent.findElements(strategy);
            if (!elements.isEmpty()) {
                return Optional.of(elements.get(0));
            }
        }
        return Optional.empty();
    }

    /** True when no strategy matches within the given timeout. */
    public boolean isAbsent(Locator locator, Duration timeout) {
        return tryResolve(locator, timeout, Visibility.ANY).isEmpty();
    }

    public Duration defaultTimeout() {
        return defaultTimeout;
    }

    // ------------------------------------------------------------------ internals

    private List<By> candidates(Locator locator) {
        // With healing disabled the suite behaves like a classic framework:
        // useful to prove that the fallbacks are what keeps it green.
        return AtlasConfig.selfHealingEnabled() ? locator.strategies() : List.of(locator.primary());
    }

    private Optional<WebElement> firstMatch(SearchContext context, By strategy, Visibility visibility) {
        try {
            for (WebElement element : context.findElements(strategy)) {
                if (visibility == Visibility.ANY || element.isDisplayed()) {
                    return Optional.of(element);
                }
            }
        } catch (StaleElementReferenceException e) {
            // The DOM changed while we were reading it: the next poll will retry.
            return Optional.empty();
        }
        return Optional.empty();
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for an element", e);
        }
    }

    /** Whether an element merely has to exist, or also to be rendered. */
    public enum Visibility {
        ANY, VISIBLE
    }
}
