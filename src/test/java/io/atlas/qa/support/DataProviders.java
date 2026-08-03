package io.atlas.qa.support;

import io.atlas.qa.core.data.ExcelDataReader;
import io.atlas.qa.core.data.JsonDataReader;
import io.atlas.qa.domain.LoginAttempt;
import org.testng.annotations.DataProvider;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Central catalogue of the data sets used by the suite.
 *
 * <p>Keeping the providers in one class means a fixture is loaded once and
 * reused, and that a test class never has to know whether the data came from
 * JSON, from a spreadsheet or from an API call.
 */
public final class DataProviders {

    private DataProviders() {
    }

    /** Negative login scenarios, one test per row, all running in parallel. */
    @DataProvider(name = "invalidLogins", parallel = true)
    public static Object[][] invalidLogins() {
        return JsonDataReader.asRows("testdata/login-attempts.json", LoginAttempt.class);
    }

    /**
     * Same idea, sourced from a spreadsheet — the format business analysts
     * actually send. The file is optional: the provider degrades to an empty set
     * instead of breaking the whole suite when it is not there.
     */
    @DataProvider(name = "checkoutScenariosFromExcel")
    public static Object[][] checkoutScenariosFromExcel() {
        Path workbook = Path.of("src/test/resources/testdata/checkout-scenarios.xlsx");
        if (!Files.exists(workbook)) {
            return new Object[0][0];
        }
        return ExcelDataReader.asRows("testdata/checkout-scenarios.xlsx", "scenarios");
    }
}
