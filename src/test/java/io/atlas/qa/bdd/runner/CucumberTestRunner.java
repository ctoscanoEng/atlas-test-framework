package io.atlas.qa.bdd.runner;

import io.atlas.qa.core.listener.SelfReporting;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * Entry point of the BDD layer.
 *
 * <p>The same page objects back both the TestNG suite and the Gherkin scenarios:
 * BDD here is a second way to <em>describe</em> the system, not a second
 * framework to maintain. Filter what runs with
 * {@code ./mvnw test -Pbdd -Dcucumber.tags="@smoke and not @authorisation"}.
 */
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"io.atlas.qa.bdd.steps", "io.atlas.qa.bdd.support"},
        plugin = {
                "pretty",
                "html:target/atlas-report/cucumber.html",
                "json:target/atlas-report/cucumber.json",
                "timeline:target/atlas-report/cucumber-timeline"
        },
        monochrome = true,
        publish = false)
public class CucumberTestRunner extends AbstractTestNGCucumberTests implements SelfReporting {

    /**
     * Scenarios run in parallel, one browser each, exactly like the TestNG suite.
     * The thread count comes from the suite file or from {@code -Datlas.threads}.
     */
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
