package io.atlas.qa.core.driver;

import io.atlas.qa.core.config.AtlasConfig;
import io.atlas.qa.core.config.BrowserType;
import io.atlas.qa.core.exception.AtlasException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Owns the browser session of the current thread.
 *
 * <h2>Why {@link ThreadLocal}</h2>
 * TestNG runs methods in parallel on a pool of threads. A single static
 * {@code WebDriver} field would have every thread driving the same browser:
 * the classic source of flaky suites that "only fail when parallel is on".
 * Binding the session to the thread makes parallel execution correct by
 * construction, and {@link #quitSession()} always calls {@code remove()} so the
 * thread-local map cannot leak when a pooled thread is reused.
 */
public final class DriverManager {

    private static final Logger LOG = LogManager.getLogger(DriverManager.class);
    private static final ThreadLocal<WebDriver> SESSION = new ThreadLocal<>();
    private static final AtomicInteger ACTIVE_SESSIONS = new AtomicInteger();

    private DriverManager() {
    }

    /** Starts a session for the current thread using the configured browser. */
    public static WebDriver startSession() {
        return startSession(AtlasConfig.browser());
    }

    /** Starts a session for the current thread with an explicit browser (cross-browser suites). */
    public static WebDriver startSession(BrowserType browser) {
        if (SESSION.get() != null) {
            LOG.warn("A session was already open on thread '{}': closing it before starting a new one",
                    Thread.currentThread().getName());
            quitSession();
        }
        WebDriver driver = DriverFactory.create(browser);
        SESSION.set(driver);
        LOG.debug("Active sessions: {}", ACTIVE_SESSIONS.incrementAndGet());
        return driver;
    }

    public static WebDriver driver() {
        WebDriver driver = SESSION.get();
        if (driver == null) {
            throw new AtlasException("""
                    No browser session bound to thread '%s'.
                    A page object or a step was used outside a test lifecycle. Make sure the test \
                    extends BaseWebTest (or that the Cucumber hooks are registered) so the session \
                    is started before use.""".formatted(Thread.currentThread().getName()));
        }
        return driver;
    }

    public static boolean hasSession() {
        return SESSION.get() != null;
    }

    /** Closes the session of the current thread; safe to call twice. */
    public static void quitSession() {
        WebDriver driver = SESSION.get();
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
        } catch (RuntimeException e) {
            // A browser that already died must not mask the real test failure.
            LOG.warn("Browser did not shut down cleanly: {}", e.getMessage());
        } finally {
            SESSION.remove();
            LOG.debug("Active sessions: {}", ACTIVE_SESSIONS.decrementAndGet());
        }
    }
}
