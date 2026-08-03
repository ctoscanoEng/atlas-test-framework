package io.atlas.qa.core.element;

import io.atlas.qa.core.exception.ConfigurationException;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A named element address made of one primary strategy and any number of
 * ordered fallbacks.
 *
 * <h2>The problem it solves</h2>
 * A {@link By} is anonymous: when it stops matching, the report shows
 * {@code By.cssSelector: #a > div:nth-child(3)} and nobody knows what that was
 * supposed to be. A {@code Locator} carries a human description, so failures
 * read like <em>"Unable to resolve element [Checkout: place order button]"</em>.
 *
 * <h2>The second problem it solves</h2>
 * Front-end frameworks regenerate ids and class names at build time. Declaring
 * fallbacks lets {@link ElementResolver} keep the test alive on a
 * <em>stable</em> alternative strategy and record the event, so the team gets a
 * list of locators to repair instead of a wall of red.
 *
 * <pre>{@code
 * static final Locator LOGIN = Locator.named("Login: submit button")
 *         .by(By.id("btn-login"))                              // fastest, most brittle
 *         .orBy(By.cssSelector("[data-testid='login-submit']")) // contract with the front-end team
 *         .orBy(By.xpath("//button[normalize-space()='Login']"))// last resort, language dependent
 *         .build();
 * }</pre>
 *
 * Instances are immutable and therefore safe as {@code static final} constants
 * shared by every parallel thread.
 */
public final class Locator {

    private final String description;
    private final List<By> strategies;

    private Locator(String description, List<By> strategies) {
        this.description = description;
        this.strategies = List.copyOf(strategies);
    }

    public static Builder named(String description) {
        return new Builder(description);
    }

    /** Compact form when the fallbacks are already known. */
    public static Locator of(String description, By primary, By... fallbacks) {
        Builder builder = named(description).by(primary);
        for (By fallback : fallbacks) {
            builder.orBy(fallback);
        }
        return builder.build();
    }

    public String description() {
        return description;
    }

    /** Primary strategy first, fallbacks in declaration order. */
    public List<By> strategies() {
        return strategies;
    }

    public By primary() {
        return strategies.get(0);
    }

    public boolean hasFallbacks() {
        return strategies.size() > 1;
    }

    public String strategiesAsText() {
        return strategies.stream().map(By::toString).collect(Collectors.joining(" | "));
    }

    /**
     * Derives a locator whose strategies are parameterised, e.g. a row of a
     * table identified by product name. Keeps the description meaningful.
     */
    public static Locator dynamic(String description, String xpathTemplate, Object... arguments) {
        return Locator.of(description.formatted(arguments), By.xpath(xpathTemplate.formatted(arguments)));
    }

    @Override
    public String toString() {
        return description + " -> " + strategiesAsText();
    }

    // ------------------------------------------------------------------ builder

    public static final class Builder {

        private final String description;
        private final List<By> strategies = new ArrayList<>();

        private Builder(String description) {
            if (description == null || description.isBlank()) {
                throw new ConfigurationException("A locator must carry a human readable description");
            }
            this.description = description;
        }

        public Builder by(By primary) {
            strategies.add(primary);
            return this;
        }

        public Builder orBy(By fallback) {
            strategies.add(fallback);
            return this;
        }

        public Locator build() {
            if (strategies.isEmpty()) {
                throw new ConfigurationException("Locator '%s' declares no strategy".formatted(description));
            }
            return new Locator(description, strategies);
        }
    }
}
