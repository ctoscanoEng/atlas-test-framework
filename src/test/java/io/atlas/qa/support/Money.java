package io.atlas.qa.support;

import io.atlas.qa.core.exception.AtlasException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Parses the amounts rendered by the application ("€ 129.90", "€ 1,204.90",
 * "1.204,90 €") into {@link BigDecimal}.
 *
 * <h2>Why not double</h2>
 * {@code 0.1 + 0.2} is not {@code 0.3} in binary floating point. A checkout test
 * that compares totals as {@code double} fails once a month on a rounding
 * artefact, and a suite that fails for reasons nobody can explain stops being
 * trusted long before it stops being run.
 *
 * <h2>Separator heuristic</h2>
 * The last separator is the decimal one <em>only</em> when it is followed by one
 * or two digits; otherwise it is a thousands separator. That single rule covers
 * both the English and the continental European rendering.
 */
public final class Money {

    private Money() {
    }

    public static BigDecimal parse(String rendered) {
        if (rendered == null || rendered.isBlank()) {
            throw new AtlasException("Cannot read an amount from an empty string");
        }

        String cleaned = rendered.replaceAll("[^0-9.,-]", "");
        int lastSeparator = Math.max(cleaned.lastIndexOf('.'), cleaned.lastIndexOf(','));

        String normalised;
        if (lastSeparator < 0) {
            normalised = cleaned;
        } else {
            String fraction = cleaned.substring(lastSeparator + 1);
            if (fraction.length() == 1 || fraction.length() == 2) {
                String units = cleaned.substring(0, lastSeparator).replaceAll("[.,]", "");
                normalised = units + "." + fraction;
            } else {
                normalised = cleaned.replaceAll("[.,]", "");
            }
        }

        try {
            return new BigDecimal(normalised).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new AtlasException("'%s' is not a readable amount".formatted(rendered), e);
        }
    }

    public static BigDecimal of(String plain) {
        return new BigDecimal(plain).setScale(2, RoundingMode.HALF_UP);
    }

    /** Rounds like a cash register: half up, two decimals. */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
