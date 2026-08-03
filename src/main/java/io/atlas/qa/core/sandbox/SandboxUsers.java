package io.atlas.qa.core.sandbox;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The user directory of the sandbox application.
 * <p>
 * Deliberately mirrors the personas that real e-commerce test suites need:
 * a happy-path user, a locked account, a user with no permissions. Keeping them
 * in one class means UI tests, API tests and BDD scenarios agree on the fixture.
 */
public final class SandboxUsers {

    public static final String STANDARD = "standard_user";
    public static final String LOCKED = "locked_user";
    public static final String READONLY = "readonly_user";
    public static final String PASSWORD = "atlas_secret";

    private static final Map<String, List<String>> ROLES = Map.of(
            STANDARD, List.of("CUSTOMER", "CHECKOUT"),
            LOCKED, List.of(),
            READONLY, List.of("CUSTOMER"));

    private static final Set<String> LOCKED_ACCOUNTS = Set.of(LOCKED);

    private SandboxUsers() {
    }

    public static boolean isValid(String username, String password) {
        return ROLES.containsKey(username) && PASSWORD.equals(password);
    }

    public static boolean isLocked(String username) {
        return LOCKED_ACCOUNTS.contains(username);
    }

    public static List<String> rolesOf(String username) {
        return ROLES.getOrDefault(username, List.of());
    }

    /** Opaque, deterministic token — enough to assert on, not pretending to be a real JWT. */
    public static String tokenFor(String username) {
        return "atlas." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(username.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
