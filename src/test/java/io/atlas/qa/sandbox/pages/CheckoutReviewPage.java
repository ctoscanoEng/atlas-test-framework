package io.atlas.qa.sandbox.pages;

import io.atlas.qa.core.element.Locator;
import io.atlas.qa.core.page.BasePage;
import io.atlas.qa.core.report.ReportManager;
import io.atlas.qa.support.Money;
import org.openqa.selenium.By;

import java.math.BigDecimal;
import java.util.List;

/** Step 2 of the checkout: order review, where the arithmetic is verified. */
public final class CheckoutReviewPage extends BasePage {

    private static final Locator STEP_MARKER = Locator.of("Checkout review: step indicator",
            By.cssSelector(".steps .active[data-testid='step-2']"));

    private static final Locator DELIVERY_SUMMARY = Locator.of("Checkout review: delivery summary",
            By.cssSelector("[data-testid='delivery-summary']"));

    private static final Locator LINES = Locator.of("Checkout review: order lines",
            By.cssSelector("[data-testid='review-line']"));

    private static final Locator LINE_NAMES = Locator.of("Checkout review: line names",
            By.cssSelector("[data-testid='review-name']"));

    private static final Locator SUBTOTAL = Locator.of("Checkout review: subtotal",
            By.cssSelector("[data-testid='subtotal']"));

    private static final Locator TAX = Locator.of("Checkout review: tax",
            By.cssSelector("[data-testid='tax']"));

    private static final Locator GRAND_TOTAL = Locator.of("Checkout review: grand total",
            By.cssSelector("[data-testid='grand-total']"));

    private static final Locator PLACE_ORDER = Locator.named("Checkout review: place order")
            .by(By.id("btn-place-order"))
            .orBy(By.cssSelector("[data-testid='place-order']"))
            .build();

    @Override
    protected Locator pageMarker() {
        return STEP_MARKER;
    }

    public String deliverySummary() {
        return $(DELIVERY_SUMMARY).text();
    }

    public int lineCount() {
        return $(LINES).count();
    }

    public List<String> lineNames() {
        return $(LINE_NAMES).allTexts();
    }

    public BigDecimal subtotal() {
        return Money.parse($(SUBTOTAL).text());
    }

    public BigDecimal tax() {
        return Money.parse($(TAX).text());
    }

    public BigDecimal total() {
        return Money.parse($(GRAND_TOTAL).text());
    }

    public OrderConfirmationPage placeOrder() {
        ReportManager.step("Placing the order for %s".formatted($(GRAND_TOTAL).text()));
        $(PLACE_ORDER).click();
        return new OrderConfirmationPage().waitUntilLoaded();
    }
}
