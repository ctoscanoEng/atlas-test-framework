package io.atlas.qa.sandbox.tests;

import io.atlas.qa.base.BaseWebTest;
import io.atlas.qa.core.element.ElementResolver;
import io.atlas.qa.core.element.HealingLedger;
import io.atlas.qa.core.element.Locator;
import io.atlas.qa.core.exception.ElementResolutionException;
import io.atlas.qa.core.sandbox.SandboxUsers;
import io.atlas.qa.sandbox.pages.LoginPage;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests <em>of the framework itself</em>.
 *
 * <p>The resolver is the component the whole suite depends on: if it silently
 * stopped falling back, every other test would still pass — until the day the
 * application changed and hundreds of them went red at once. These three tests
 * pin the behaviour down, exactly as one would for any other piece of
 * production code.
 */
public class LocatorResilienceTests extends BaseWebTest {

    @Test(groups = {"smoke", "framework"},
            description = "The application regenerates element ids at every load — the premise of the feature")
    public void theApplicationReallyRegeneratesIds() {
        LoginPage.open();

        String firstLoad = idOfSubmitButton();
        driver().navigate().refresh();
        new LoginPage().waitUntilLoaded();
        String secondLoad = idOfSubmitButton();

        assertThat(firstLoad).startsWith("btn-login-");
        assertThat(secondLoad).startsWith("btn-login-");
        assertThat(firstLoad)
                .as("the id must differ between two loads, which is what breaks id-based locators")
                .isNotEqualTo(secondLoad);
    }

    @Test(groups = {"smoke", "framework"},
            description = "A locator whose primary strategy is dead is healed by its fallback and recorded")
    public void brokenPrimaryStrategyIsHealedAndAudited() {
        LoginPage.open().signInAs(SandboxUsers.STANDARD, SandboxUsers.PASSWORD);

        assertThat(HealingLedger.currentTestEvents())
                .as("the sign-in button is declared with a dead id and must have been healed")
                .isNotEmpty()
                .anyMatch(event -> event.locator().equals("Login: sign in button"));

        HealingLedger.currentTestEvents().stream()
                .filter(event -> event.locator().equals("Login: sign in button"))
                .findFirst()
                .ifPresent(event -> {
                    assertThat(event.failed()).contains("btn-login");
                    assertThat(event.healedWith()).contains("data-testid");
                    assertThat(event.position()).as("the first fallback is the one that matched").isEqualTo(1);
                });
    }

    @Test(groups = {"regression", "framework"},
            description = "Without fallbacks the same lookup fails, and the error names the element")
    public void aLocatorWithoutFallbacksFailsWithAReadableMessage() {
        LoginPage.open();

        ElementResolver impatient = new ElementResolver(driver(), Duration.ofSeconds(2));
        Locator noFallback = Locator.of("Login: sign in button (no fallback declared)", By.id("btn-login"));

        assertThatThrownBy(() -> impatient.resolve(noFallback))
                .isInstanceOf(ElementResolutionException.class)
                .hasMessageContaining("Login: sign in button (no fallback declared)")
                .hasMessageContaining("Strategies attempted");
    }

    private String idOfSubmitButton() {
        return driver().findElement(By.cssSelector("[data-testid='login-submit']")).getDomAttribute("id");
    }
}
