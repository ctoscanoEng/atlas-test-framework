package io.atlas.qa.sandbox.pages;

import io.atlas.qa.core.element.Locator;
import io.atlas.qa.core.page.BasePage;
import io.atlas.qa.core.report.ReportManager;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * Sign-in page of the Atlas Outdoor back office.
 *
 * <h2>Read the SUBMIT locator</h2>
 * Its primary strategy is {@code By.id("btn-login")} and it <em>always</em>
 * fails: the application rewrites that id at every page load. The declared
 * fallback on {@code data-testid} keeps the suite green and every occurrence is
 * written to the locator backlog at the end of the run. This is the behaviour of
 * the resolver demonstrated on a real page rather than described in a comment.
 */
public final class LoginPage extends BasePage {

    private static final Locator FORM = Locator.of("Login: form",
            By.cssSelector("[data-testid='login-form']"));

    private static final Locator USERNAME = Locator.named("Login: username field")
            .by(By.id("username"))
            .orBy(By.cssSelector("[data-testid='username']"))
            .build();

    private static final Locator PASSWORD = Locator.named("Login: password field")
            .by(By.id("password"))
            .orBy(By.cssSelector("[data-testid='password']"))
            .build();

    private static final Locator SUBMIT = Locator.named("Login: sign in button")
            .by(By.id("btn-login"))                                   // regenerated on every load
            .orBy(By.cssSelector("[data-testid='login-submit']"))     // contract with the front-end team
            .orBy(By.xpath("//form[@id='login-form']//button[@type='submit']"))
            .build();

    private static final Locator ERROR = Locator.of("Login: error banner",
            By.cssSelector("[data-testid='error-message']"));

    @Override
    protected Locator pageMarker() {
        return FORM;
    }

    public static LoginPage open() {
        LoginPage page = new LoginPage();
        page.open("/index.html");
        return page.waitUntilLoaded();
    }

    /** Signs in and lands on the catalogue. Fails fast if the credentials are rejected. */
    public InventoryPage signInAs(String username, String password) {
        ReportManager.step("Signing in as '%s'".formatted(username));
        fillCredentials(username, password);
        $(SUBMIT).click();
        return new InventoryPage().waitUntilLoaded();
    }

    /** Signs in expecting the application to refuse: stays on this page. */
    public LoginPage signInExpectingRejection(String username, String password) {
        fillCredentials(username, password);
        $(SUBMIT).click();
        $(ERROR).waitUntilVisible();
        return this;
    }

    public LoginPage fillCredentials(String username, String password) {
        $(USERNAME).type(username);
        $(PASSWORD).type(password);
        return this;
    }

    public String errorMessage() {
        return $(ERROR).text();
    }

    public boolean isErrorDisplayed() {
        return $(ERROR).isVisibleWithin(Duration.ofSeconds(2));
    }
}
