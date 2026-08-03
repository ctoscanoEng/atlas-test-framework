package io.atlas.qa.core.report;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import io.atlas.qa.core.config.AtlasConfig;
import io.atlas.qa.core.config.ConfigLoader;
import io.atlas.qa.core.element.HealingLedger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Thread-safe facade over ExtentReports.
 *
 * <h2>Contract</h2>
 * Reporting is an <em>optional</em> concern: page objects and helpers call
 * {@link #step(String)} freely, and when no report is bound to the current
 * thread (unit-style execution, a helper used from {@code main}) the call is a
 * no-op. A framework whose page objects only work inside a report is a
 * framework nobody can reuse.
 */
public final class ReportManager {

    private static final Logger LOG = LogManager.getLogger(ReportManager.class);
    private static final ThreadLocal<ExtentTest> CURRENT = new ThreadLocal<>();

    private static volatile ExtentReports extent;

    private ReportManager() {
    }

    // ------------------------------------------------------------------ lifecycle

    public static synchronized ExtentReports reports() {
        if (extent == null) {
            extent = build();
        }
        return extent;
    }

    private static ExtentReports build() {
        Path output = Path.of(AtlasConfig.reportDirectory(), "index.html");
        try {
            Files.createDirectories(output.getParent());
        } catch (IOException e) {
            LOG.warn("Could not create the report directory: {}", e.getMessage());
        }

        ExtentSparkReporter spark = new ExtentSparkReporter(output.toFile());
        spark.config().setTheme(Theme.DARK);
        spark.config().setDocumentTitle(AtlasConfig.reportTitle());
        spark.config().setReportName(AtlasConfig.reportTitle());
        spark.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");

        ExtentReports reports = new ExtentReports();
        reports.attachReporter(spark);

        // The header of the report answers "how was this run configured?" —
        // the first question anyone asks when looking at a failure they did not run.
        reports.setSystemInfo("Environment", AtlasConfig.environment().key());
        reports.setSystemInfo("Browser", AtlasConfig.browser().key());
        reports.setSystemInfo("Headless", String.valueOf(AtlasConfig.headless()));
        reports.setSystemInfo("Execution", AtlasConfig.remote() ? "Selenium Grid @ " + AtlasConfig.gridUrl() : "Local");
        reports.setSystemInfo("Self-healing locators", String.valueOf(AtlasConfig.selfHealingEnabled()));
        reports.setSystemInfo("Retry policy", AtlasConfig.retryCount() + " retry per failed test");
        reports.setSystemInfo("Java", System.getProperty("java.version"));
        reports.setSystemInfo("OS", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        reports.setSystemInfo("Base URL", safeBaseUrl());

        LOG.info("HTML report will be written to {}", output.toAbsolutePath());
        return reports;
    }

    private static String safeBaseUrl() {
        try {
            return AtlasConfig.baseUrl();
        } catch (RuntimeException e) {
            return ConfigLoader.get("baseUrl", "n/a");
        }
    }

    public static void startTest(String name, String description, String... categories) {
        ExtentTest test = reports().createTest(name, description);
        for (String category : categories) {
            test.assignCategory(category);
        }
        test.assignAuthor(ConfigLoader.get("report.author", "QA Automation"));
        CURRENT.set(test);
    }

    public static void endTest() {
        CURRENT.remove();
        HealingLedger.clearCurrentTest();
    }

    public static synchronized void flush() {
        if (extent != null) {
            extent.flush();
            LOG.info("Report flushed to {}", Path.of(AtlasConfig.reportDirectory(), "index.html").toAbsolutePath());
        }
    }

    // ------------------------------------------------------------------ logging

    public static void step(String message) {
        log(Status.INFO, message);
    }

    public static void pass(String message) {
        log(Status.PASS, message);
    }

    public static void warn(String message) {
        log(Status.WARNING, message);
    }

    public static void skip(String message) {
        log(Status.SKIP, message);
    }

    public static void fail(String message) {
        log(Status.FAIL, message);
    }

    public static void fail(Throwable throwable) {
        ExtentTest test = CURRENT.get();
        if (test != null) {
            test.fail(throwable);
        }
    }

    public static void attachScreenshot(String base64, String title) {
        ExtentTest test = CURRENT.get();
        if (test == null || base64 == null || base64.isBlank()) {
            return;
        }
        test.info(title, MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
    }

    /** Publishes the healing events collected while this test was running. */
    public static void attachHealingEvents() {
        HealingLedger.currentTestEvents().forEach(event -> warn(event.asReportLine()));
    }

    private static void log(Status status, String message) {
        ExtentTest test = CURRENT.get();
        if (test == null) {
            LOG.debug("[no report bound] {}", message);
            return;
        }
        test.log(status, message);
    }
}
