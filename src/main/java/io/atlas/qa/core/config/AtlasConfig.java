package io.atlas.qa.core.config;

import io.atlas.qa.core.sandbox.SandboxServer;

import java.time.Duration;

/**
 * Typed, intention-revealing facade over {@link ConfigLoader}.
 * <p>
 * Test and page code never touches raw string keys: it asks
 * {@code AtlasConfig.explicitTimeout()} and gets a {@link Duration}. Renaming a
 * key or changing a default is therefore a one-line change contained here.
 */
public final class AtlasConfig {

    private AtlasConfig() {
    }

    // ------------------------------------------------------------------ target

    public static TargetEnvironment environment() {
        return TargetEnvironment.from(ConfigLoader.get("env", TargetEnvironment.SANDBOX.key()));
    }

    /**
     * Base URL of the application under test.
     * <p>
     * In {@link TargetEnvironment#SANDBOX} the value is not read from a file:
     * the in-process web server binds an ephemeral port, so the URL is only
     * known at runtime. This is what allows several suites to run in parallel
     * on the same CI agent without a port clash.
     */
    public static String baseUrl() {
        if (environment() == TargetEnvironment.SANDBOX) {
            return SandboxServer.instance().baseUrl();
        }
        return ConfigLoader.require("baseUrl");
    }

    public static String apiBaseUrl() {
        if (environment() == TargetEnvironment.SANDBOX) {
            return SandboxServer.instance().url("/api");
        }
        return ConfigLoader.require("api.baseUrl");
    }

    // ------------------------------------------------------------------ browser

    public static BrowserType browser() {
        return BrowserType.from(ConfigLoader.get("browser", BrowserType.CHROME.key()));
    }

    public static boolean headless() {
        return ConfigLoader.getBoolean("headless", true);
    }

    public static boolean remote() {
        return ConfigLoader.getBoolean("remote", false);
    }

    public static String gridUrl() {
        return ConfigLoader.get("gridUrl", "http://localhost:4444/wd/hub");
    }

    public static String windowSize() {
        return ConfigLoader.get("window.size", "1920,1080");
    }

    // ------------------------------------------------------------------ timing

    public static Duration explicitTimeout() {
        return Duration.ofSeconds(ConfigLoader.getInt("timeout.explicit", 15));
    }

    public static Duration pageLoadTimeout() {
        return Duration.ofSeconds(ConfigLoader.getInt("timeout.pageLoad", 40));
    }

    public static Duration scriptTimeout() {
        return Duration.ofSeconds(ConfigLoader.getInt("timeout.script", 30));
    }

    public static Duration pollingInterval() {
        return Duration.ofMillis(ConfigLoader.getInt("timeout.pollingMillis", 200));
    }

    // ------------------------------------------------------------------ policy

    public static boolean selfHealingEnabled() {
        return ConfigLoader.getBoolean("locator.selfHealing", true);
    }

    public static int retryCount() {
        return ConfigLoader.getInt("retry.count", 1);
    }

    public static boolean screenshotOnFailure() {
        return ConfigLoader.getBoolean("report.screenshotOnFailure", true);
    }

    public static boolean screenshotOnStep() {
        return ConfigLoader.getBoolean("report.screenshotOnStep", false);
    }

    public static String reportDirectory() {
        return ConfigLoader.get("report.directory", "target/atlas-report");
    }

    public static String reportTitle() {
        return ConfigLoader.get("report.title", "ATLAS Automation Report");
    }

    // ------------------------------------------------------------------ credentials

    public static String username() {
        return ConfigLoader.require("credentials.username");
    }

    public static String password() {
        return ConfigLoader.require("credentials.password");
    }
}
