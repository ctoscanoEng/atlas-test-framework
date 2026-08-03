package io.atlas.qa.core.driver.options;

import io.atlas.qa.core.config.BrowserType;
import org.openqa.selenium.remote.AbstractDriverOptions;

/**
 * Strategy that builds the capabilities of one browser.
 * <p>
 * Adding a browser means adding one implementation and registering it in
 * {@link OptionsRegistry}: no existing class is modified. That is the
 * open/closed principle applied to the part of a framework that historically
 * degenerates into a 200-line {@code if/else} chain.
 */
public interface BrowserOptionsProvider {

    /** The browser this provider is responsible for. */
    BrowserType browser();

    /**
     * @param headless whether the browser must run without a visible window
     * @return fully configured capabilities, valid both locally and on a Grid
     */
    AbstractDriverOptions<?> build(boolean headless);
}
