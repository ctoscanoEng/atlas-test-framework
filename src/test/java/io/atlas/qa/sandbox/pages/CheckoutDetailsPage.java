package io.atlas.qa.sandbox.pages;

import io.atlas.qa.core.element.Locator;
import io.atlas.qa.core.page.BasePage;
import io.atlas.qa.core.report.ReportManager;
import io.atlas.qa.domain.Customer;
import org.openqa.selenium.By;

import java.time.Duration;

/** Step 1 of the checkout: delivery details. */
public final class CheckoutDetailsPage extends BasePage {

    private static final Locator FORM = Locator.of("Checkout details: form",
            By.cssSelector("[data-testid='checkout-form']"));

    private static final Locator FIRST_NAME = Locator.of("Checkout details: first name",
            By.cssSelector("[data-testid='first-name']"));

    private static final Locator LAST_NAME = Locator.of("Checkout details: last name",
            By.cssSelector("[data-testid='last-name']"));

    private static final Locator EMAIL = Locator.of("Checkout details: email",
            By.cssSelector("[data-testid='email']"));

    private static final Locator ADDRESS = Locator.of("Checkout details: address",
            By.cssSelector("[data-testid='address']"));

    private static final Locator CITY = Locator.of("Checkout details: city",
            By.cssSelector("[data-testid='city']"));

    private static final Locator POSTCODE = Locator.of("Checkout details: postcode",
            By.cssSelector("[data-testid='postcode']"));

    private static final Locator CONTINUE = Locator.named("Checkout details: continue")
            .by(By.id("btn-continue"))
            .orBy(By.cssSelector("[data-testid='continue']"))
            .build();

    private static final Locator ERROR = Locator.of("Checkout details: error banner",
            By.cssSelector("[data-testid='error-message']"));

    @Override
    protected Locator pageMarker() {
        return FORM;
    }

    public CheckoutDetailsPage fill(Customer customer) {
        ReportManager.step("Filling the delivery details for %s".formatted(customer.fullName()));
        $(FIRST_NAME).type(customer.firstName());
        $(LAST_NAME).type(customer.lastName());
        $(EMAIL).type(customer.email());
        $(ADDRESS).type(customer.address());
        $(CITY).type(customer.city());
        $(POSTCODE).type(customer.postcode());
        return this;
    }

    /** Types only the fields the caller provided: used to prove the validation rules. */
    public CheckoutDetailsPage fillPartially(String firstName, String lastName, String email, String postcode) {
        $(FIRST_NAME).type(firstName);
        $(LAST_NAME).type(lastName);
        $(EMAIL).type(email);
        $(POSTCODE).type(postcode);
        return this;
    }

    public CheckoutReviewPage submit() {
        $(CONTINUE).click();
        return new CheckoutReviewPage().waitUntilLoaded();
    }

    public CheckoutDetailsPage submitExpectingRejection() {
        $(CONTINUE).click();
        $(ERROR).waitUntilVisible();
        return this;
    }

    public String errorMessage() {
        return $(ERROR).text();
    }

    public boolean isErrorDisplayed() {
        return $(ERROR).isVisibleWithin(Duration.ofSeconds(2));
    }
}
