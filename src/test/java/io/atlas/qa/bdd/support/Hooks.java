package io.atlas.qa.bdd.support;

import io.atlas.qa.core.driver.DriverManager;
import io.atlas.qa.core.report.ReportManager;
import io.atlas.qa.core.report.ScreenshotService;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.ThreadContext;

/**
 * Lifecycle of a BDD scenario: it does exactly what {@code BaseWebTest} does for
 * a TestNG test, so both entry points share one browser policy and one report.
 */
public class Hooks {

    @Before(order = 0)
    public void startSession(Scenario scenario) {
        ThreadContext.put("testName", scenario.getName());
        DriverManager.startSession();
        ReportManager.startTest(scenario.getName(),
                "Feature: " + featureOf(scenario),
                scenario.getSourceTagNames().toArray(String[]::new));
    }

    @After(order = 100)
    public void publishEvidence(Scenario scenario) {
        ReportManager.attachHealingEvents();

        if (scenario.isFailed() && DriverManager.hasSession()) {
            ScreenshotService.captureBase64(DriverManager.driver()).ifPresent(image -> {
                ReportManager.attachScreenshot(image, "State of the page at failure");
                // Also attached to the native Cucumber report, for teams that read that one.
                scenario.attach(java.util.Base64.getDecoder().decode(image), "image/png", scenario.getName());
            });
            ReportManager.fail("Scenario failed: " + scenario.getName());
        } else {
            ReportManager.pass("Scenario passed: " + scenario.getName());
        }
        ReportManager.endTest();
    }

    @After(order = 0)
    public void closeSession() {
        DriverManager.quitSession();
        ThreadContext.remove("testName");
    }

    /** "classpath:features/checkout.feature" → "checkout". */
    private String featureOf(Scenario scenario) {
        String uri = scenario.getUri().toString();
        return uri.substring(uri.lastIndexOf('/') + 1).replace(".feature", "");
    }
}
