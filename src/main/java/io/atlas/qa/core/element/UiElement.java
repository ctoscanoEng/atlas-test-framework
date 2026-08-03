package io.atlas.qa.core.element;

import io.atlas.qa.core.report.ReportManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Fluent, self-defending wrapper around a {@link WebElement}.
 *
 * <h2>Lazy by design</h2>
 * A {@code UiElement} holds a {@link Locator}, never a {@code WebElement}. The
 * element is resolved again at every single interaction, which structurally
 * removes {@code StaleElementReferenceException} from single-page applications
 * that re-render on each state change.
 *
 * <h2>Escalating interaction</h2>
 * A click is attempted natively; if something overlays the target the element is
 * scrolled into the viewport and retried; only as a last resort a scripted click
 * is issued — and it is written to the report as a warning, because a test that
 * needs JavaScript to click is testing something a user could not do.
 */
public final class UiElement {

    private static final Logger LOG = LogManager.getLogger(UiElement.class);
    private static final int MAX_ATTEMPTS = 3;

    private final WebDriver driver;
    private final ElementResolver resolver;
    private final Locator locator;

    private UiElement(WebDriver driver, ElementResolver resolver, Locator locator) {
        this.driver = driver;
        this.resolver = resolver;
        this.locator = locator;
    }

    public static UiElement of(WebDriver driver, ElementResolver resolver, Locator locator) {
        return new UiElement(driver, resolver, locator);
    }

    public Locator locator() {
        return locator;
    }

    // ------------------------------------------------------------------ actions

    public UiElement click() {
        return perform("click", element -> {
            try {
                element.click();
            } catch (ElementNotInteractableException e) {  // ElementClickIntercepted is a subtype
                scrollIntoView(element);
                try {
                    element.click();
                } catch (ElementNotInteractableException retried) {
                    LOG.warn("'{}' is covered by another element: falling back to a scripted click", locator.description());
                    ReportManager.warn("Scripted click used on '%s' (element was not directly clickable)"
                            .formatted(locator.description()));
                    javascript().executeScript("arguments[0].click();", element);
                }
            }
            return null;
        });
    }

    /** Clears the field and types the value; empty input is treated as "clear only". */
    public UiElement type(CharSequence text) {
        return perform("type '%s'".formatted(mask(text)), element -> {
            element.clear();
            if (text != null && !text.isEmpty()) {
                element.sendKeys(text);
            }
            return null;
        });
    }

    public UiElement typeAndConfirm(CharSequence text) {
        type(text);
        return perform("press ENTER", element -> {
            element.sendKeys(Keys.ENTER);
            return null;
        });
    }

    public UiElement hover() {
        return perform("hover", element -> {
            new Actions(driver).moveToElement(element).perform();
            return null;
        });
    }

    public UiElement selectByVisibleText(String option) {
        return perform("select '%s'".formatted(option), element -> {
            new Select(element).selectByVisibleText(option);
            return null;
        });
    }

    public UiElement setCheckbox(boolean checked) {
        return perform("set checkbox to " + checked, element -> {
            if (element.isSelected() != checked) {
                element.click();
            }
            return null;
        });
    }

    public UiElement scrollIntoView() {
        return perform("scroll into view", element -> {
            scrollIntoView(element);
            return null;
        });
    }

    /** Uploads a file by writing the absolute path into the input, no OS dialog involved. */
    public UiElement uploadFile(String absolutePath) {
        return perform("upload " + absolutePath, element -> {
            element.sendKeys(absolutePath);
            return null;
        });
    }

    // ------------------------------------------------------------------ queries

    public String text() {
        return read("read text", WebElement::getText).trim();
    }

    public String value() {
        return attribute("value");
    }

    public String attribute(String name) {
        return read("read attribute " + name, element -> {
            String domAttribute = element.getDomAttribute(name);
            return domAttribute != null ? domAttribute : element.getDomProperty(name);
        });
    }

    public boolean isVisible() {
        return resolver.tryResolve(locator, resolver.defaultTimeout(), ElementResolver.Visibility.VISIBLE).isPresent();
    }

    /** Fast negative check: does not burn the full timeout when the element is expected to be absent. */
    public boolean isVisibleWithin(Duration timeout) {
        return resolver.tryResolve(locator, timeout, ElementResolver.Visibility.VISIBLE).isPresent();
    }

    public boolean isEnabled() {
        return read("read enabled state", WebElement::isEnabled);
    }

    public boolean isSelected() {
        return read("read selected state", WebElement::isSelected);
    }

    public boolean isAbsent(Duration timeout) {
        return resolver.isAbsent(locator, timeout);
    }

    public int count() {
        return resolver.resolveAll(locator).size();
    }

    public List<String> allTexts() {
        return resolver.resolveAll(locator).stream().map(WebElement::getText).map(String::trim).toList();
    }

    public WebElement unwrap() {
        return resolver.resolve(locator);
    }

    // ------------------------------------------------------------------ waits

    public UiElement waitUntilVisible() {
        resolver.resolveVisible(locator);
        return this;
    }

    public UiElement waitUntilGone(Duration timeout) {
        if (!resolver.isAbsent(locator, timeout)) {
            throw new io.atlas.qa.core.exception.AtlasException(
                    "Element '%s' was still present after %d ms".formatted(locator.description(), timeout.toMillis()));
        }
        return this;
    }

    // ------------------------------------------------------------------ internals

    private UiElement perform(String action, Function<WebElement, Void> interaction) {
        execute(action, interaction);
        return this;
    }

    private <T> T read(String action, Function<WebElement, T> query) {
        return execute(action, query);
    }

    /**
     * Resolves and interacts, retrying when the DOM is replaced underneath us.
     * Anything other than staleness is propagated immediately: swallowing real
     * failures is how frameworks start hiding bugs.
     */
    private <T> T execute(String action, Function<WebElement, T> interaction) {
        StaleElementReferenceException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                WebElement element = resolver.resolveVisible(locator);
                T result = interaction.apply(element);
                LOG.debug("{} on '{}'", action, locator.description());
                ReportManager.step("%s on '%s'".formatted(action, locator.description()));
                return result;
            } catch (StaleElementReferenceException e) {
                lastFailure = e;
                LOG.debug("'{}' went stale during '{}' (attempt {}/{}), resolving again",
                        locator.description(), action, attempt, MAX_ATTEMPTS);
            }
        }
        throw new io.atlas.qa.core.exception.AtlasException(
                "'%s' kept being re-rendered while trying to %s (%d attempts)"
                        .formatted(locator.description(), action, MAX_ATTEMPTS), lastFailure);
    }

    private void scrollIntoView(WebElement element) {
        javascript().executeScript("arguments[0].scrollIntoView({block:'center', behavior:'instant'});", element);
    }

    private JavascriptExecutor javascript() {
        return (JavascriptExecutor) driver;
    }

    /** Passwords must never reach a log file or an HTML report. */
    private String mask(CharSequence text) {
        String description = locator.description().toLowerCase();
        boolean sensitive = description.contains("password") || description.contains("secret")
                || description.contains("token") || description.contains("card");
        if (text == null) {
            return "";
        }
        return sensitive ? "*".repeat(Math.min(text.length(), 8)) : text.toString();
    }

    @Override
    public String toString() {
        return locator.description();
    }
}
