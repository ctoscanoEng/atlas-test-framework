package io.atlas.qa.core.driver.options;

import io.atlas.qa.core.config.AtlasConfig;
import io.atlas.qa.core.config.BrowserType;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.AbstractDriverOptions;

public final class FirefoxOptionsProvider implements BrowserOptionsProvider {

    @Override
    public BrowserType browser() {
        return BrowserType.FIREFOX;
    }

    @Override
    public AbstractDriverOptions<?> build(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();

        if (headless) {
            options.addArguments("-headless");
        }
        String[] size = AtlasConfig.windowSize().split(",");
        options.addArguments("--width=" + size[0].trim(), "--height=" + size[1].trim());

        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("dom.webnotifications.enabled", false);
        profile.setPreference("browser.download.folderList", 2);
        profile.setPreference("intl.accept_languages", "en-US, en");
        options.setProfile(profile);

        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.setAcceptInsecureCerts(true);
        return options;
    }
}
