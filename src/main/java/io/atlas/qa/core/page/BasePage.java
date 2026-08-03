package io.atlas.qa.core.page;

import io.atlas.qa.core.config.AtlasConfig;
import io.atlas.qa.core.driver.DriverManager;
import io.atlas.qa.core.element.ElementResolver;
import io.atlas.qa.core.element.Locator;
import io.atlas.qa.core.element.UiElement;
import io.atlas.qa.core.element.Waits;
import io.atlas.qa.core.exception.AtlasException;
import io.atlas.qa.core.report.ReportManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * Contract shared by every page object.
 *
 * <h2>Rules this class enforces</h2>
 * <ul>
 *   <li>a page object never owns a driver instance — it asks
 *       {@link DriverManager} for the session of the current thread, so the same
 *       page class is safe in a suite running eight browsers at once;</li>
 *   <li>every page declares a {@link #pageMarker()}: the one element that proves
 *       the page is really displayed. {@link #waitUntilLoaded()} makes
 *       "am I on the right page?" a framework concern instead of a copy-pasted
 *       assertion;</li>
 *   <li>page objects expose <em>business</em> actions and never leak
 *       {@code WebElement} to the tests.</li>
 * </ul>
 */
public abstract class BasePage {

    protected final Logger log = LogManager.getLogger(getClass());
    protected final WebDriver driver;
    protected final ElementResolver resolver;

    protected BasePage() {
        this(DriverManager.driver());
    }

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.resolver = new ElementResolver(driver);
    }

    /** The element whose presence proves this page is displayed. */
    protected abstract Locator pageMarker();

    /** Human readable name, used in reports and error messages. */
    public String pageName() {
        return getClass().getSimpleName().replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    // ------------------------------------------------------------------ element access

    /** Binds a locator to the current session; the element itself is resolved lazily. */
    protected UiElement $(Locator locator) {
        return UiElement.of(driver, resolver, locator);
    }

    // ------------------------------------------------------------------ lifecycle

    public boolean isLoaded() {
        return resolver.tryResolve(pageMarker(), AtlasConfig.explicitTimeout(), ElementResolver.Visibility.VISIBLE)
                .isPresent();
    }

    /**
     * Blocks until the marker element is displayed and returns {@code this}
     * already typed as the concrete page, so factory methods read
     * {@code return new CartPage().waitUntilLoaded();} without a cast.
     */
    @SuppressWarnings("unchecked")
    public <T extends BasePage> T waitUntilLoaded() {
        if (!isLoaded()) {
            throw new AtlasException("""
                    Expected to be on '%s' but its marker element was never displayed.
                    Current URL   : %s
                    Current title : %s
                    Marker        : %s""".formatted(pageName(), driver.getCurrentUrl(), driver.getTitle(),
                    pageMarker()));
        }
        ReportManager.step("Page '%s' is displayed".formatted(pageName()));
        log.debug("'{}' loaded at {}", pageName(), driver.getCurrentUrl());
        return (T) this;
    }

    /** Navigates to a path relative to the configured base URL. */
    protected void open(String path) {
        String url = AtlasConfig.baseUrl() + (path.startsWith("/") ? path : "/" + path);
        driver.get(url);
        Waits.untilPageIsIdle(driver);
    }

    // ------------------------------------------------------------------ browser surface

    public String title() {
        return driver.getTitle();
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }

    protected void switchToFrame(Locator frame) {
        driver.switchTo().frame(resolver.resolve(frame));
        log.debug("Switched into frame '{}'", frame.description());
    }

    protected void switchToMainDocument() {
        driver.switchTo().defaultContent();
    }

    /**
     * Reaches inside a web component. Shadow DOM is invisible to XPath and to
     * ordinary CSS, so the only supported route is the shadow root itself.
     */
    protected WebElement inShadowRoot(Locator host, By insideShadow) {
        SearchContext shadowRoot = resolver.resolve(host).getShadowRoot();
        return shadowRoot.findElement(insideShadow);
    }

    protected String acceptAlertAndReadText() {
        Alert alert = Waits.untilAlertPresent(driver);
        String text = alert.getText();
        alert.accept();
        ReportManager.step("Accepted browser dialog: \"%s\"".formatted(text));
        return text;
    }

    protected void dismissAlert() {
        Waits.untilAlertPresent(driver).dismiss();
    }

    protected Object javascript(String script, Object... arguments) {
        return ((JavascriptExecutor) driver).executeScript(script, arguments);
    }

    protected void waitUntil(java.util.function.Supplier<Boolean> condition, String description) {
        Waits.until(driver, condition, description);
    }

    protected Duration shortTimeout() {
        return Duration.ofSeconds(2);
    }
}
