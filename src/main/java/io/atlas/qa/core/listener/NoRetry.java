package io.atlas.qa.core.listener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opts a test out of the retry policy.
 *
 * <p>Applied to tests that must never be re-run automatically: anything that
 * creates data on a shared system, or a test written specifically to prove a
 * defect exists.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface NoRetry {
}
