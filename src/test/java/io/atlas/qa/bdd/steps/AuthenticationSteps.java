package io.atlas.qa.bdd.steps;

import io.atlas.qa.bdd.support.ScenarioContext;
import io.atlas.qa.sandbox.pages.InventoryPage;
import io.atlas.qa.sandbox.pages.LoginPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps of the authentication feature.
 *
 * <p>Every step is one line of delegation to a page object. A step definition
 * that contains locators or waits is a page object in disguise — and the day two
 * features need the same interaction, it gets copied instead of reused.
 */
public class AuthenticationSteps {

    private final ScenarioContext context;

    public AuthenticationSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("the sign-in page is open")
    public void theSignInPageIsOpen() {
        context.put("loginPage", LoginPage.open());
    }

    @Given("I am signed in as {string}")
    public void iAmSignedInAs(String username) {
        InventoryPage catalogue = LoginPage.open().signInAs(username, "atlas_secret");
        context.put("catalogue", catalogue);
    }

    @When("I sign in as {string} with the password {string}")
    public void iSignInAs(String username, String password) {
        LoginPage page = context.get("loginPage", LoginPage.class);
        if ("standard_user".equals(username) && "atlas_secret".equals(password)) {
            context.put("catalogue", page.signInAs(username, password));
        } else {
            context.put("loginPage", page.signInExpectingRejection(username, password));
        }
    }

    @Then("the catalogue is displayed")
    public void theCatalogueIsDisplayed() {
        assertThat(context.get("catalogue", InventoryPage.class).isLoaded()).isTrue();
    }

    @Then("the header shows that {string} is signed in")
    public void theHeaderShows(String username) {
        assertThat(context.get("catalogue", InventoryPage.class).signedInUser()).isEqualTo(username);
    }

    @Then("the sign-in page shows {string}")
    public void theSignInPageShows(String message) {
        assertThat(context.get("loginPage", LoginPage.class).errorMessage()).contains(message);
    }

    @Then("I am still on the sign-in page")
    public void iAmStillOnTheSignInPage() {
        assertThat(context.get("loginPage", LoginPage.class).currentUrl()).endsWith("index.html");
    }
}
