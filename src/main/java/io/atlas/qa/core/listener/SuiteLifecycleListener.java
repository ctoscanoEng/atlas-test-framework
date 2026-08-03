package io.atlas.qa.core.listener;

import io.atlas.qa.core.config.AtlasConfig;
import io.atlas.qa.core.config.ConfigLoader;
import io.atlas.qa.core.config.TargetEnvironment;
import io.atlas.qa.core.element.HealingLedger;
import io.atlas.qa.core.report.ReportManager;
import io.atlas.qa.core.sandbox.SandboxServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ISuite;
import org.testng.ISuiteListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Suite-level bookkeeping: starts the application under test, prints the
 * effective configuration, flushes the report and publishes the locator
 * maintenance backlog.
 *
 * <p>The backlog file is the deliverable that turns self-healing from a party
 * trick into an engineering practice: at the end of every run the team gets a
 * markdown list of the locators that only survived thanks to a fallback.
 */
public class SuiteLifecycleListener implements ISuiteListener {

    private static final Logger LOG = LogManager.getLogger(SuiteLifecycleListener.class);

    @Override
    public void onStart(ISuite suite) {
        LOG.info("""

                ══════════════════════════════════════════════════════════════
                 ATLAS — suite '{}'
                ══════════════════════════════════════════════════════════════""", suite.getName());

        ConfigLoader.snapshot().forEach((key, value) ->
                LOG.info("  {} = {}", key, key.toLowerCase().contains("password") ? "********" : value));

        if (AtlasConfig.environment() == TargetEnvironment.SANDBOX) {
            // Starting it here rather than lazily gives a clean failure before
            // any browser is launched if the port cannot be bound.
            LOG.info("  application under test = {}", SandboxServer.instance().baseUrl());
        }
        ReportManager.reports();
    }

    @Override
    public void onFinish(ISuite suite) {
        ReportManager.flush();
        publishHealingBacklog();
        LOG.info("Suite '{}' finished", suite.getName());
    }

    private void publishHealingBacklog() {
        List<HealingLedger.HealingEvent> events = HealingLedger.runEvents();
        if (events.isEmpty()) {
            LOG.info("Locator health: every element was found with its primary strategy");
            return;
        }

        Map<String, Long> byLocator = events.stream()
                .collect(Collectors.groupingBy(HealingLedger.HealingEvent::locator, Collectors.counting()));

        StringBuilder markdown = new StringBuilder("""
                # Locator maintenance backlog

                The following elements could not be found with their primary strategy during the
                last run. The suite stayed green thanks to the declared fallbacks, but each entry
                is technical debt: the primary strategy must be repaired or removed.

                | Element | Times healed | Recovered with |
                |---|---:|---|
                """);
        byLocator.forEach((locator, count) -> {
            String healedWith = events.stream()
                    .filter(event -> event.locator().equals(locator))
                    .map(HealingLedger.HealingEvent::healedWith)
                    .findFirst().orElse("n/a");
            markdown.append("| %s | %d | `%s` |%n".formatted(locator, count, healedWith));
        });

        LOG.warn("Locator health: {} element(s) required a fallback during this run", byLocator.size());
        try {
            Path file = Path.of(AtlasConfig.reportDirectory(), "locator-backlog.md");
            Files.createDirectories(file.getParent());
            Files.writeString(file, markdown.toString());
            LOG.warn("Maintenance backlog written to {}", file.toAbsolutePath());
        } catch (IOException e) {
            LOG.warn("Could not write the maintenance backlog: {}", e.getMessage());
        }
    }
}
