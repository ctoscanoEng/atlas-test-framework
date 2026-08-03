package io.atlas.qa.core.driver.options;

import io.atlas.qa.core.config.AtlasConfig;
import io.atlas.qa.core.config.BrowserType;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;

import java.util.List;

public final class EdgeOptionsProvider implements BrowserOptionsProvider {

    @Override
    public BrowserType browser() {
        return BrowserType.EDGE;
    }

    @Override
    public AbstractDriverOptions<?> build(boolean headless) {
        EdgeOptions options = new EdgeOptions();

        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments(
                "--window-size=" + AtlasConfig.windowSize(),
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-notifications",
                "--lang=en-US");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));

        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.setAcceptInsecureCerts(true);
        return options;
    }
}
