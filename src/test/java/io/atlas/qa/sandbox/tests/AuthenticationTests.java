package io.atlas.qa.sandbox.tests;

import io.atlas.qa.base.BaseWebTest;
import io.atlas.qa.core.sandbox.SandboxUsers;
import io.atlas.qa.domain.LoginAttempt;
import io.atlas.qa.sandbox.pages.InventoryPage;
import io.atlas.qa.sandbox.pages.LoginPage;
import io.atlas.qa.support.DataProviders;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authentication rules of the back office.
 *
 * <p>Every assertion is written with AssertJ so a failure reads like a sentence:
 * <em>"expecting the catalogue to be displayed but the sign-in form was still
 * visible"</em> beats {@code expected:&lt;true&gt; but was:&lt;false&gt;} in a
 * pipeline log nobody was watching.
 */
public class AuthenticationTests extends BaseWebTest {

    @Test(groups = {"smoke", "authentication"},
            description = "A valid account reaches the catalogue and is named in the header")
    public void standardUserSignsIn() {
        InventoryPage catalogue = LoginPage.open()
                .signInAs(SandboxUsers.STANDARD, SandboxUsers.PASSWORD);

        assertThat(catalogue.signedInUser())
                .as("username displayed in the header after signing in")
                .isEqualTo(SandboxUsers.STANDARD);
        assertThat(catalogue.currentUrl()).endsWith("inventory.html");
        assertThat(catalogue.productCount()).as("products rendered in the catalogue").isPositive();
    }

    @Test(dataProvider = "invalidLogins", dataProviderClass = DataProviders.class,
            groups = {"regression", "authentication"},
            description = "Every rejected sign-in shows the user an actionable message")
    public void rejectedSignIn(LoginAttempt attempt) {
        LoginPage page = LoginPage.open()
                .signInExpectingRejection(attempt.username(), attempt.password());

        assertThat(page.errorMessage())
                .as("message shown for the scenario: %s", attempt.scenario())
                .contains(attempt.expectedMessage());
        assertThat(page.currentUrl())
                .as("the browser must stay on the sign-in page")
                .endsWith("index.html");
    }

    @Test(groups = {"regression", "authentication"},
            description = "Signing out clears the session and protects the internal pages")
    public void signedOutUserCannotReachTheCatalogue() {
        LoginPage.open().signInAs(SandboxUsers.STANDARD, SandboxUsers.PASSWORD);

        // Simulate a user who bookmarked an internal page and comes back later.
        driver().manage().deleteAllCookies();
        ((org.openqa.selenium.JavascriptExecutor) driver()).executeScript("sessionStorage.clear();");
        driver().navigate().refresh();

        LoginPage login = new LoginPage();
        assertThat(login.isLoaded())
                .as("an expired session must land back on the sign-in page")
                .isTrue();
        assertThat(login.errorMessage()).contains("session has expired");
    }
}
