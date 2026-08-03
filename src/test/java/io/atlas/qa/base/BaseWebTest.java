package io.atlas.qa.base;

import io.atlas.qa.core.config.AtlasConfig;
import io.atlas.qa.core.config.BrowserType;
import io.atlas.qa.core.driver.DriverManager;
import io.atlas.qa.core.report.ReportManager;
import io.atlas.qa.sandbox.pages.LoginPage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/**
 * Lifecycle shared by every UI test.
 *
 * <h2>One browser per test method</h2>
 * A fresh session per method costs a second and buys complete isolation: no
 * cookie, no local storage entry and no leftover navigation can leak from one
 * test into the next. Suites that share a browser between tests are fast until
 * the day they start failing in an order-dependent way that nobody can debug.
 *
 * <h2>The browser parameter</h2>
 * {@code @Parameters("browser")} lets a suite file run the same classes on
 * Chrome, Firefox and Edge in parallel; when absent, the configured default is
 * used, so the class is equally runnable from Eclipse with "Run as > TestNG Test".
 */
public abstract class BaseWebTest {

    protected final Logger log = LogManager.getLogger(getClass());

    @BeforeMethod(alwaysRun = true)
    @Parameters("browser")
    public void startBrowser(@Optional String browser) {
        BrowserType type = (browser == null || browser.isBlank())
                ? AtlasConfig.browser()
                : BrowserType.from(browser);
        DriverManager.startSession(type);
        ReportManager.step("Browser session started on %s (%s)"
                .formatted(type.key(), AtlasConfig.remote() ? "Grid" : "local"));
    }

    @AfterMethod(alwaysRun = true)
    public void closeBrowser() {
        DriverManager.quitSession();
    }

    protected WebDriver driver() {
        return DriverManager.driver();
    }

    /** Entry point of the application under test. */
    protected LoginPage openApplication() {
        return LoginPage.open();
    }
}
