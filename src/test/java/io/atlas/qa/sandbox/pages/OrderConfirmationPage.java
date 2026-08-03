package io.atlas.qa.sandbox.pages;

import io.atlas.qa.core.element.Locator;
import io.atlas.qa.core.page.BasePage;
import io.atlas.qa.support.Money;
import org.openqa.selenium.By;

import java.math.BigDecimal;

/** Step 3 of the checkout: confirmation and order reference. */
public final class OrderConfirmationPage extends BasePage {

    private static final Locator TITLE = Locator.of("Confirmation: title",
            By.cssSelector("[data-testid='confirmation-title']"));

    private static final Locator REFERENCE = Locator.of("Confirmation: order reference",
            By.cssSelector("[data-testid='order-reference']"));

    private static final Locator ITEMS = Locator.of("Confirmation: item count",
            By.cssSelector("[data-testid='order-items']"));

    private static final Locator TOTAL = Locator.of("Confirmation: amount charged",
            By.cssSelector("[data-testid='order-total']"));

    private static final Locator NEW_ORDER = Locator.of("Confirmation: start a new order",
            By.cssSelector("[data-testid='new-order']"));

    @Override
    protected Locator pageMarker() {
        return TITLE;
    }

    public String heading() {
        return $(TITLE).text();
    }

    public String orderReference() {
        return $(REFERENCE).text();
    }

    public int itemCount() {
        return Integer.parseInt($(ITEMS).text());
    }

    public BigDecimal amountCharged() {
        return Money.parse($(TOTAL).text());
    }

    public InventoryPage startNewOrder() {
        $(NEW_ORDER).click();
        return new InventoryPage().waitUntilLoaded();
    }
}
