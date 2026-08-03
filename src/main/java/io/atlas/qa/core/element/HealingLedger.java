package io.atlas.qa.core.element;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Audit trail of every locator that had to fall back to an alternative
 * strategy.
 *
 * <p>Self-healing without accounting is dangerous: the suite stays green while
 * the page objects silently rot. The ledger keeps two views:
 * <ul>
 *   <li><b>per-thread</b> — attached to the report of the test being executed;</li>
 *   <li><b>per-run</b> — printed at the end of the suite as a maintenance
 *       backlog: "these locators must be repaired".</li>
 * </ul>
 */
public final class HealingLedger {

    private static final Logger LOG = LogManager.getLogger(HealingLedger.class);

    private static final ThreadLocal<List<HealingEvent>> CURRENT_TEST =
            ThreadLocal.withInitial(ArrayList::new);
    private static final Queue<HealingEvent> RUN = new ConcurrentLinkedQueue<>();

    private HealingLedger() {
    }

    /**
     * @param locator      the element that could not be found with its primary strategy
     * @param failed       strategies that returned nothing, in order
     * @param healedWith   the strategy that actually matched
     * @param position     index of the winning strategy (1 = first fallback)
     */
    public record HealingEvent(String locator, String failed, String healedWith, int position, Instant at) {

        public String asReportLine() {
            return "[HEALED] %s — primary strategy failed (%s), recovered with fallback #%d (%s)"
                    .formatted(locator, failed, position, healedWith);
        }
    }

    static void record(HealingEvent event) {
        CURRENT_TEST.get().add(event);
        RUN.add(event);
        LOG.warn(event.asReportLine());
    }

    /** Events collected while the current test was running. */
    public static List<HealingEvent> currentTestEvents() {
        return List.copyOf(CURRENT_TEST.get());
    }

    /** Called by the listener between tests so events are never attributed twice. */
    public static void clearCurrentTest() {
        CURRENT_TEST.remove();
    }

    /** Every event of the whole run, ordered by arrival. */
    public static List<HealingEvent> runEvents() {
        return List.copyOf(RUN);
    }

    public static boolean isEmpty() {
        return RUN.isEmpty();
    }
}
