package io.atlas.qa.core.driver;

import io.atlas.qa.core.config.AtlasConfig;
import io.atlas.qa.core.config.BrowserType;
import io.atlas.qa.core.driver.options.OptionsRegistry;
import io.atlas.qa.core.exception.AtlasException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.openqa.selenium.support.events.EventFiringDecorator;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;

/**
 * Single place where a {@link WebDriver} is born.
 * <p>
 * Responsibilities, in order:
 * <ol>
 *   <li>pick the capabilities strategy for the requested browser;</li>
 *   <li>instantiate either a local driver or a {@link RemoteWebDriver} against
 *       a Selenium Grid — the test code cannot tell the difference;</li>
 *   <li>apply the timeout policy;</li>
 *   <li>decorate the instance with the event logger.</li>
 * </ol>
 *
 * <p>Driver binaries are resolved by <em>Selenium Manager</em>, shipped with
 * Selenium itself: no third-party driver-manager dependency, nothing to install
 * on the CI agent, no chromedriver checked into the repository.
 */
public final class DriverFactory {

    private static final Logger LOG = LogManager.getLogger(DriverFactory.class);

    private DriverFactory() {
    }

    public static WebDriver create(BrowserType browser) {
        boolean headless = AtlasConfig.headless();
        AbstractDriverOptions<?> options = OptionsRegistry.providerFor(browser).build(headless);

        WebDriver driver = AtlasConfig.remote()
                ? createRemote(browser, options)
                : createLocal(browser, options);

        applyTimeouts(driver);
        if (!headless && browser == BrowserType.SAFARI) {
            driver.manage().window().maximize();
        }

        LOG.info("Session started | browser={} | headless={} | remote={}", browser.key(), headless, AtlasConfig.remote());
        return new EventFiringDecorator<>(new WebDriverEventLogger()).decorate(driver);
    }

    private static WebDriver createLocal(BrowserType browser, AbstractDriverOptions<?> options) {
        return switch (browser) {
            case CHROME -> new ChromeDriver((ChromeOptions) options);
            case FIREFOX -> new FirefoxDriver((FirefoxOptions) options);
            case EDGE -> new EdgeDriver((EdgeOptions) options);
            case SAFARI -> new SafariDriver((SafariOptions) options);
        };
    }

    private static WebDriver createRemote(BrowserType browser, AbstractDriverOptions<?> options) {
        String gridUrl = AtlasConfig.gridUrl();
        try {
            URL url = URI.create(gridUrl).toURL();
            RemoteWebDriver driver = new RemoteWebDriver(url, options);
            // Without this, uploading a file to a browser running in another
            // container silently fails: the path only exists on the client.
            driver.setFileDetector(new LocalFileDetector());
            LOG.info("Remote session {} negotiated on {} for {}", driver.getSessionId(), gridUrl, browser.key());
            return driver;
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new AtlasException("Invalid Selenium Grid URL: " + gridUrl, e);
        } catch (RuntimeException e) {
            throw new AtlasException(
                    "Unable to obtain a %s session from the Grid at %s. Is it running? (docker compose -f docker/docker-compose.grid.yml up -d)"
                            .formatted(browser.key(), gridUrl), e);
        }
    }

    /**
     * Implicit waits are set to zero on purpose. Mixing implicit and explicit
     * waits produces unpredictable timeouts; ATLAS waits exclusively through
     * {@code Waits} and the locator resolver.
     */
    private static void applyTimeouts(WebDriver driver) {
        driver.manage().timeouts()
                .implicitlyWait(Duration.ZERO)
                .pageLoadTimeout(AtlasConfig.pageLoadTimeout())
                .scriptTimeout(AtlasConfig.scriptTimeout());
    }
}
