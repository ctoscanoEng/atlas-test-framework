package io.atlas.qa.sandbox.pages;

import io.atlas.qa.core.element.Locator;
import io.atlas.qa.core.page.BasePage;
import io.atlas.qa.core.report.ReportManager;
import io.atlas.qa.support.Money;
import org.openqa.selenium.By;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/** Shopping cart: lines, totals and the entry point of the checkout flow. */
public final class CartPage extends BasePage {

    private static final Locator TITLE = Locator.of("Cart: page title",
            By.cssSelector("[data-testid='page-title']"));

    private static final Locator LINES = Locator.of("Cart: lines",
            By.cssSelector("[data-testid='cart-line']"));

    private static final Locator LINE_NAMES = Locator.of("Cart: line names",
            By.cssSelector("[data-testid='line-name']"));

    private static final Locator EMPTY_MESSAGE = Locator.of("Cart: empty message",
            By.cssSelector("[data-testid='empty-cart']"));

    private static final Locator SUBTOTAL = Locator.of("Cart: subtotal",
            By.cssSelector("[data-testid='subtotal']"));

    private static final Locator TAX = Locator.of("Cart: tax",
            By.cssSelector("[data-testid='tax']"));

    private static final Locator GRAND_TOTAL = Locator.of("Cart: grand total",
            By.cssSelector("[data-testid='grand-total']"));

    private static final Locator CHECKOUT = Locator.named("Cart: proceed to checkout")
            .by(By.id("btn-checkout"))
            .orBy(By.cssSelector("[data-testid='checkout']"))
            .build();

    private static final Locator ERROR = Locator.of("Cart: error banner",
            By.cssSelector("[data-testid='error-message']"));

    private static final Locator CONTINUE_SHOPPING = Locator.of("Cart: continue shopping",
            By.cssSelector("[data-testid='continue-shopping']"));

    @Override
    protected Locator pageMarker() {
        return TITLE;
    }

    // ------------------------------------------------------------------ queries

    public List<String> lineNames() {
        return $(LINE_NAMES).allTexts();
    }

    public int lineCount() {
        return $(LINES).count();
    }

    public boolean isEmpty() {
        return $(EMPTY_MESSAGE).isVisibleWithin(Duration.ofSeconds(2));
    }

    public int quantityOf(String productName) {
        return Integer.parseInt($(Locator.dynamic("Cart: quantity of '%s'",
                "//div[@data-testid='cart-line'][.//*[@data-testid='line-name']"
                        + "[normalize-space()='%s']]//*[@data-testid='line-quantity']",
                productName)).text());
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

    public boolean isCheckoutEnabled() {
        return $(CHECKOUT).isEnabled();
    }

    public String errorMessage() {
        return $(ERROR).text();
    }

    public boolean isErrorDisplayed() {
        return $(ERROR).isVisibleWithin(Duration.ofSeconds(2));
    }

    // ------------------------------------------------------------------ actions

    public CartPage removeLine(String productName) {
        ReportManager.step("Removing '%s' from the cart".formatted(productName));
        int before = lineCount();
        $(Locator.dynamic("Cart: remove '%s'",
                "//div[@data-testid='cart-line'][.//*[@data-testid='line-name']"
                        + "[normalize-space()='%s']]//button[@data-testid='remove-line']",
                productName)).click();
        waitUntil(() -> lineCount() == before - 1, "the line for '%s' to disappear".formatted(productName));
        return this;
    }

    public CheckoutDetailsPage proceedToCheckout() {
        $(CHECKOUT).click();
        return new CheckoutDetailsPage().waitUntilLoaded();
    }

    /** Used by the read-only account scenario, where the checkout must be refused. */
    public CartPage attemptCheckout() {
        $(CHECKOUT).click();
        return this;
    }

    public InventoryPage continueShopping() {
        $(CONTINUE_SHOPPING).click();
        return new InventoryPage().waitUntilLoaded();
    }
}
