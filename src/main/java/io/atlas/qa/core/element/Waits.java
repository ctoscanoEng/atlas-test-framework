package io.atlas.qa.core.element;

import io.atlas.qa.core.config.AtlasConfig;
import org.openqa.selenium.Alert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Explicit synchronisation helpers.
 *
 * <p>{@code Thread.sleep} does not appear anywhere in this framework. Every wait
 * is a condition with a deadline: the suite is as fast as the application on a
 * good day and still stable on a bad one.
 */
public final class Waits {

    private Waits() {
    }

    public static FluentWait<WebDriver> fluent(WebDriver driver, Duration timeout) {
        return new FluentWait<>(driver)
                .withTimeout(timeout)
                .pollingEvery(AtlasConfig.pollingInterval())
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
    }

    public static void untilUrlContains(WebDriver driver, String fragment) {
        fluent(driver, AtlasConfig.explicitTimeout())
                .withMessage("URL never contained '%s' (last seen: %s)".formatted(fragment, driver.getCurrentUrl()))
                .until(ExpectedConditions.urlContains(fragment));
    }

    public static void untilTitleIs(WebDriver driver, String title) {
        fluent(driver, AtlasConfig.explicitTimeout()).until(ExpectedConditions.titleIs(title));
    }

    /**
     * Waits for the document to be parsed and for any jQuery/fetch activity to
     * settle. Guarded: an application without jQuery must not throw.
     */
    public static void untilPageIsIdle(WebDriver driver) {
        fluent(driver, AtlasConfig.pageLoadTimeout()).until(d -> {
            Object state = ((JavascriptExecutor) d).executeScript(
                    """
                    var jqueryIdle = (typeof window.jQuery === 'undefined') || window.jQuery.active === 0;
                    var fetchIdle  = (typeof window.__atlasPendingRequests === 'undefined')
                                     || window.__atlasPendingRequests === 0;
                    return (document.readyState === 'complete') && jqueryIdle && fetchIdle;
                    """);
            return Boolean.TRUE.equals(state);
        });
    }

    public static Alert untilAlertPresent(WebDriver driver) {
        return fluent(driver, AtlasConfig.explicitTimeout()).until(ExpectedConditions.alertIsPresent());
    }

    public static void untilNumberOfWindowsIs(WebDriver driver, int expected) {
        fluent(driver, AtlasConfig.explicitTimeout()).until(ExpectedConditions.numberOfWindowsToBe(expected));
    }

    /**
     * Generic bridge for domain conditions:
     * {@code Waits.until(driver, () -> cart.badgeCount() == 3, "cart shows 3 items")}.
     */
    public static void until(WebDriver driver, Supplier<Boolean> condition, String description) {
        try {
            fluent(driver, AtlasConfig.explicitTimeout())
                    .withMessage(description)
                    .until((ExpectedCondition<Boolean>) d -> condition.get());
        } catch (TimeoutException e) {
            throw new io.atlas.qa.core.exception.AtlasException(
                    "Condition never satisfied: " + description, e);
        }
    }
}
