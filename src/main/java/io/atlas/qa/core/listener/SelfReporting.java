package io.atlas.qa.core.listener;

/**
 * Marks a TestNG class that publishes its own report entries.
 *
 * <p>The Cucumber runner exposes a single TestNG method ({@code runScenario})
 * for every scenario in the suite. Left alone, the listener would file a hundred
 * report entries all called "runScenario". Implementing this interface tells
 * {@link TestReportListener} to stand back, and the Cucumber hooks report the
 * scenario under its real, business-readable name instead.
 */
public interface SelfReporting {
}
