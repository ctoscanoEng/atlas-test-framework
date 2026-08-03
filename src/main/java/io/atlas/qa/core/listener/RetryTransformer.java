package io.atlas.qa.core.listener;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Attaches {@link RetryAnalyzer} to every test at runtime.
 *
 * <p>The alternative — writing {@code @Test(retryAnalyzer = RetryAnalyzer.class)}
 * on 400 methods — is exactly the kind of duplication that rots: one forgotten
 * annotation and a whole class behaves differently from the rest of the suite.
 * Here the policy is declared once, in the suite file, and applied globally;
 * {@link NoRetry} is the explicit escape hatch.
 */
@SuppressWarnings("rawtypes") // TestNG declares the callback with raw types
public class RetryTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor constructor, Method method) {
        boolean optedOut = method != null
                && (method.isAnnotationPresent(NoRetry.class)
                    || method.getDeclaringClass().isAnnotationPresent(NoRetry.class));

        // TestNG does not return null when no analyzer is declared: it returns
        // its own DisabledRetryAnalyzer placeholder.
        Class<?> declared = annotation.getRetryAnalyzerClass();
        boolean noneDeclared = declared == null || "DisabledRetryAnalyzer".equals(declared.getSimpleName());

        if (!optedOut && noneDeclared) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }
}
