package io.atlas.qa.core.listener;

import io.atlas.qa.core.config.AtlasConfig;
import io.atlas.qa.core.driver.DriverManager;
import io.atlas.qa.core.report.ReportManager;
import io.atlas.qa.core.report.ScreenshotService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Turns raw TestNG events into a readable narrative: report entries, evidence
 * on failure and a per-test logging context.
 *
 * <p>{@link ThreadContext} tags every log line with the test name, which is the
 * only way to read a log file produced by eight browsers running in parallel.
 */
public class TestReportListener implements ITestListener {

    private static final Logger LOG = LogManager.getLogger(TestReportListener.class);

    @Override
    public void onTestStart(ITestResult result) {
        if (reportsItself(result)) {
            return;
        }
        String name = displayName(result);
        ThreadContext.put("testName", name);
        ReportManager.startTest(name, description(result), groups(result));
        LOG.info("──────── START {} ────────", name);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        if (reportsItself(result)) {
            return;
        }
        ReportManager.attachHealingEvents();
        ReportManager.pass("Test passed in %d ms".formatted(duration(result)));
        LOG.info("──────── PASS  {} ({} ms) ────────", displayName(result), duration(result));
        ReportManager.endTest();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        if (reportsItself(result)) {
            return;
        }
        String name = displayName(result);
        ReportManager.attachHealingEvents();
        ReportManager.fail("Test failed after %d ms".formatted(duration(result)));
        ReportManager.fail(result.getThrowable());

        if (AtlasConfig.screenshotOnFailure() && DriverManager.hasSession()) {
            ScreenshotService.captureBase64(DriverManager.driver())
                    .ifPresent(image -> ReportManager.attachScreenshot(image, "State of the page at failure"));
            ScreenshotService.captureToFile(DriverManager.driver(), name);
            ScreenshotService.capturePageSource(DriverManager.driver(), name);
        }
        LOG.error("──────── FAIL  {} ────────", name, result.getThrowable());
        ReportManager.endTest();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        if (reportsItself(result)) {
            return;
        }
        ReportManager.skip("Skipped: " + reasonFor(result));
        LOG.warn("──────── SKIP  {} — {}", displayName(result), reasonFor(result));
        ReportManager.endTest();
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        ReportManager.warn("Test failed but stayed within the accepted success percentage");
    }

    // ------------------------------------------------------------------ helpers

    /** Cucumber scenarios are reported by their own hooks, under their real name. */
    private boolean reportsItself(ITestResult result) {
        return result.getInstance() instanceof SelfReporting;
    }

    /** Data-driven tests are indistinguishable in a report unless the data is in the name. */
    private String displayName(ITestResult result) {
        Object[] parameters = result.getParameters();
        String base = result.getMethod().getMethodName();
        if (parameters == null || parameters.length == 0) {
            return base;
        }
        return base + " [" + Arrays.stream(parameters)
                .map(String::valueOf)
                .map(value -> value.length() > 40 ? value.substring(0, 40) + "…" : value)
                .collect(Collectors.joining(", ")) + "]";
    }

    private String description(ITestResult result) {
        String description = result.getMethod().getDescription();
        return (description == null || description.isBlank()) ? "No description provided" : description;
    }

    private String[] groups(ITestResult result) {
        return result.getMethod().getGroups();
    }

    private long duration(ITestResult result) {
        return result.getEndMillis() - result.getStartMillis();
    }

    private String reasonFor(ITestResult result) {
        Throwable throwable = result.getThrowable();
        return throwable == null ? "a dependency of this test did not pass" : throwable.getMessage();
    }
}
