package io.atlas.qa.core.report;

import io.atlas.qa.core.config.AtlasConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Captures evidence at the moment of failure.
 *
 * <p>Every method is failure-tolerant: a browser that has already crashed
 * cannot produce a screenshot, and that secondary problem must never replace
 * the original assertion error in the report.
 */
public final class ScreenshotService {

    private static final Logger LOG = LogManager.getLogger(ScreenshotService.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private ScreenshotService() {
    }

    /** Base64 payload, embedded directly in the HTML report (single portable file). */
    public static Optional<String> captureBase64(WebDriver driver) {
        try {
            return Optional.of(((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64));
        } catch (RuntimeException e) {
            LOG.warn("Screenshot could not be captured: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** PNG on disk, for CI systems that archive artefacts. */
    public static Optional<Path> captureToFile(WebDriver driver, String testName) {
        try {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Path directory = Path.of(AtlasConfig.reportDirectory(), "screenshots");
            Files.createDirectories(directory);
            Path file = directory.resolve("%s_%s.png".formatted(sanitise(testName), LocalDateTime.now().format(STAMP)));
            Files.write(file, png);
            LOG.info("Failure evidence saved to {}", file.toAbsolutePath());
            return Optional.of(file);
        } catch (RuntimeException | IOException e) {
            LOG.warn("Screenshot could not be written to disk: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** The page source at failure time: invaluable when the screenshot looks fine. */
    public static Optional<Path> capturePageSource(WebDriver driver, String testName) {
        try {
            Path directory = Path.of(AtlasConfig.reportDirectory(), "page-source");
            Files.createDirectories(directory);
            Path file = directory.resolve("%s_%s.html".formatted(sanitise(testName), LocalDateTime.now().format(STAMP)));
            Files.writeString(file, driver.getPageSource() == null ? "" : driver.getPageSource());
            return Optional.of(file);
        } catch (RuntimeException | IOException e) {
            LOG.warn("Page source could not be written to disk: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static String sanitise(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
