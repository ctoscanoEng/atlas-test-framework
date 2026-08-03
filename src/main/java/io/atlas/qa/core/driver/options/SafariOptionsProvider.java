package io.atlas.qa.core.driver.options;

import io.atlas.qa.core.config.BrowserType;
import io.atlas.qa.core.exception.ConfigurationException;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.safari.SafariOptions;

public final class SafariOptionsProvider implements BrowserOptionsProvider {

    @Override
    public BrowserType browser() {
        return BrowserType.SAFARI;
    }

    @Override
    public AbstractDriverOptions<?> build(boolean headless) {
        if (headless) {
            // Failing loudly beats failing mysteriously: safaridriver has no
            // headless mode, so a suite asking for one is misconfigured.
            throw new ConfigurationException(
                    "Safari does not support headless execution. Run with -Datlas.headless=false "
                            + "or pick another browser with -Datlas.browser=chrome.");
        }
        SafariOptions options = new SafariOptions();
        options.setAutomaticInspection(false);
        return options;
    }
}
