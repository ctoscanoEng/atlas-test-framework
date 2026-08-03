package io.atlas.qa.core.driver.options;

import io.atlas.qa.core.config.AtlasConfig;
import io.atlas.qa.core.config.BrowserType;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;

import java.util.List;
import java.util.Map;

public final class ChromeOptionsProvider implements BrowserOptionsProvider {

    @Override
    public BrowserType browser() {
        return BrowserType.CHROME;
    }

    @Override
    public AbstractDriverOptions<?> build(boolean headless) {
        ChromeOptions options = new ChromeOptions();

        if (headless) {
            // The "new" headless mode shares the rendering path of headed Chrome:
            // it avoids the classic "green locally, red in CI" layout differences.
            options.addArguments("--headless=new");
        }
        options.addArguments(
                "--window-size=" + AtlasConfig.windowSize(),
                "--disable-gpu",
                "--no-sandbox",                       // required inside containers
                "--disable-dev-shm-usage",            // avoids /dev/shm exhaustion in Docker
                "--disable-extensions",
                "--disable-notifications",
                "--disable-popup-blocking",
                "--remote-allow-origins=*",
                "--lang=en-US");

        // Silence the "Chrome is being controlled by automated software" banner and
        // the password manager bubble, both of which can steal focus and clicks.
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("prefs", Map.of(
                "credentials_enable_service", false,
                "profile.password_manager_enabled", false,
                "profile.default_content_setting_values.notifications", 2));

        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.setAcceptInsecureCerts(true);
        return options;
    }
}
