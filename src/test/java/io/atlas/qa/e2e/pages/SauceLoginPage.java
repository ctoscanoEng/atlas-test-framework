package io.atlas.qa.e2e.pages;

import io.atlas.qa.core.config.AtlasConfig;
import io.atlas.qa.core.element.Locator;
import io.atlas.qa.core.page.BasePage;
import org.openqa.selenium.By;

/**
 * Sign-in page of a third-party demo store.
 *
 * <p>Its only purpose is to prove that the framework core is not coupled to the
 * sandbox: same {@code BasePage}, same resolver, same reporting, a different
 * application and a different environment overlay — and not one line changed in
 * {@code src/main/java}.
 */
public final class SauceLoginPage extends BasePage {

    private static final Locator FORM = Locator.of("SauceDemo: login form",
            By.cssSelector("form"));

    private static final Locator USERNAME = Locator.named("SauceDemo: username")
            .by(By.id("user-name"))
            .orBy(By.cssSelector("[data-test='username']"))
            .build();

    private static final Locator PASSWORD = Locator.named("SauceDemo: password")
            .by(By.id("password"))
            .orBy(By.cssSelector("[data-test='password']"))
            .build();

    private static final Locator SUBMIT = Locator.named("SauceDemo: login button")
            .by(By.id("login-button"))
            .orBy(By.cssSelector("[data-test='login-button']"))
            .build();

    private static final Locator ERROR = Locator.of("SauceDemo: error banner",
            By.cssSelector("[data-test='error']"));

    @Override
    protected Locator pageMarker() {
        return FORM;
    }

    public static SauceLoginPage open() {
        SauceLoginPage page = new SauceLoginPage();
        page.open("/");
        return page.waitUntilLoaded();
    }

    public SauceStorePage signInWithConfiguredCredentials() {
        return signInAs(AtlasConfig.username(), AtlasConfig.password());
    }

    public SauceStorePage signInAs(String username, String password) {
        $(USERNAME).type(username);
        $(PASSWORD).type(password);
        $(SUBMIT).click();
        return new SauceStorePage().waitUntilLoaded();
    }

    public String errorMessage() {
        return $(ERROR).text();
    }
}
