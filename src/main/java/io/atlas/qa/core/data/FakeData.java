package io.atlas.qa.core.data;

import io.atlas.qa.core.config.ConfigLoader;
import net.datafaker.Faker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.Random;

/**
 * Synthetic but <em>reproducible</em> test data.
 *
 * <h2>The seed matters</h2>
 * Random data finds bugs that fixed fixtures never will — and makes failures
 * impossible to reproduce, which is why most teams abandon it. ATLAS seeds the
 * generator from configuration and prints the seed in the log and in the report
 * header: re-running with {@code -Datlas.data.seed=<value>} replays exactly the
 * same data set.
 */
public final class FakeData {

    private static final Logger LOG = LogManager.getLogger(FakeData.class);
    private static final long SEED = resolveSeed();
    private static final ThreadLocal<Faker> FAKER =
            ThreadLocal.withInitial(() -> new Faker(Locale.UK, new Random(SEED)));

    private FakeData() {
    }

    public static long seed() {
        return SEED;
    }

    public static Faker faker() {
        return FAKER.get();
    }

    public static String firstName() {
        return faker().name().firstName();
    }

    public static String lastName() {
        return faker().name().lastName();
    }

    public static String fullName() {
        return faker().name().fullName();
    }

    /** Always on a domain that cannot receive mail: no test ever emails a real person. */
    public static String email() {
        return "%s.%s.%d@atlas.test".formatted(
                firstName().toLowerCase().replaceAll("[^a-z]", ""),
                lastName().toLowerCase().replaceAll("[^a-z]", ""),
                faker().number().numberBetween(1000, 9999));
    }

    public static String phone() {
        return faker().phoneNumber().subscriberNumber(9);
    }

    public static String streetAddress() {
        return faker().address().streetAddress();
    }

    public static String city() {
        return faker().address().city();
    }

    public static String postcode() {
        return faker().address().zipCode();
    }

    public static String company() {
        return faker().company().name();
    }

    private static long resolveSeed() {
        long seed = Long.parseLong(ConfigLoader.get("data.seed", String.valueOf(System.currentTimeMillis())));
        LOG.info("Synthetic data seed = {} (replay this run with -Datlas.data.seed={})", seed, seed);
        return seed;
    }
}
