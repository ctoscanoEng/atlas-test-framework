package io.atlas.qa.core.driver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.events.WebDriverListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Cross-cutting trace of every WebDriver interaction, attached through
 * {@code EventFiringDecorator}.
 * <p>
 * This is the difference between a failure report that says
 * {@code NoSuchElementException} and one that shows the last five actions that
 * led to it. The listener never throws: an observability concern must not be
 * able to fail a test.
 */
public final class WebDriverEventLogger implements WebDriverListener {

    private static final Logger LOG = LogManager.getLogger("WebDriver");

    @Override
    public void afterGet(WebDriver driver, String url) {
        LOG.info("navigated to {}", url);
    }

    @Override
    public void beforeClick(WebElement element) {
        LOG.debug("click on {}", describe(element));
    }

    @Override
    public void beforeSendKeys(WebElement element, CharSequence... keysToSend) {
        LOG.debug("type into {}", describe(element));
    }

    @Override
    public void beforeQuit(WebDriver driver) {
        LOG.debug("closing browser session");
    }

    @Override
    public void onError(Object target, Method method, Object[] args, InvocationTargetException e) {
        LOG.warn("WebDriver call {} failed: {}", method.getName(),
                e.getTargetException() == null ? e.getMessage() : e.getTargetException().getMessage());
    }

    /** Best-effort description; any failure here is irrelevant to the test outcome. */
    private String describe(WebElement element) {
        try {
            return element.toString();
        } catch (RuntimeException ignored) {
            return "<element>";
        }
    }
}
