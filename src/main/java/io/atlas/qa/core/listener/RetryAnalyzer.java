package io.atlas.qa.core.listener;

import io.atlas.qa.core.config.AtlasConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriverException;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Re-runs a failed test — but only when the failure looks environmental.
 *
 * <h2>The opinion encoded here</h2>
 * Blindly retrying everything is how teams stop trusting their suite: a real
 * regression that fails 1 time out of 2 becomes invisible. ATLAS retries
 * {@link WebDriverException} and infrastructure errors (session lost, network
 * reset, Grid node evicted) and never retries an {@link AssertionError}: if the
 * application produced the wrong value, that is a defect, and it must stay red.
 *
 * <p>Retries are also capped and always written to the report, so "it passed on
 * the second attempt" is visible rather than hidden.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger LOG = LogManager.getLogger(RetryAnalyzer.class);

    private int attempts;

    @Override
    public boolean retry(ITestResult result) {
        int maxRetries = AtlasConfig.retryCount();
        if (attempts >= maxRetries) {
            return false;
        }
        Throwable cause = result.getThrowable();
        if (!isRetryable(cause)) {
            LOG.info("'{}' failed on an assertion: not retried by design", result.getName());
            return false;
        }
        attempts++;
        LOG.warn("Retrying '{}' ({}/{}) after an environmental failure: {}",
                result.getName(), attempts, maxRetries, cause == null ? "unknown" : cause.getClass().getSimpleName());
        return true;
    }

    private boolean isRetryable(Throwable cause) {
        if (cause == null) {
            return false;
        }
        if (cause instanceof AssertionError) {
            return false;
        }
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (current instanceof WebDriverException
                    || current instanceof java.net.SocketException
                    || current instanceof java.io.IOException) {
                return true;
            }
        }
        return false;
    }
}
