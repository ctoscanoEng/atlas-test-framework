package io.atlas.qa.sandbox.pages;

import io.atlas.qa.core.element.Locator;
import io.atlas.qa.core.page.BasePage;
import io.atlas.qa.core.report.ReportManager;
import io.atlas.qa.support.Money;
import org.openqa.selenium.By;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/**
 * Product catalogue.
 *
 * <p>The grid is rendered after an HTTP call with a deliberately variable
 * latency, so every method here waits on a condition. There is not a single
 * {@code Thread.sleep} in this class — nor anywhere else in the project.
 */
public final class InventoryPage extends BasePage {

    private static final Locator GRID = Locator.of("Catalogue: product grid",
            By.cssSelector("[data-testid='product-grid']"));

    private static final Locator SPINNER = Locator.of("Catalogue: loading indicator",
            By.cssSelector("[data-testid='loading-indicator']"));

    private static final Locator PRODUCT_CARDS = Locator.of("Catalogue: product cards",
            By.cssSelector("[data-testid='product-card']"));

    private static final Locator PRODUCT_NAMES = Locator.of("Catalogue: product names",
            By.cssSelector("[data-testid='product-name']"));

    private static final Locator PRODUCT_PRICES = Locator.of("Catalogue: product prices",
            By.cssSelector("[data-testid='product-price']"));

    private static final Locator CATEGORY_FILTER = Locator.named("Catalogue: category filter")
            .by(By.id("filter-category"))
            .orBy(By.cssSelector("[data-testid='filter-category']"))
            .build();

    private static final Locator SORT_ORDER = Locator.named("Catalogue: sort order")
            .by(By.id("sort-order"))
            .orBy(By.cssSelector("[data-testid='sort-order']"))
            .build();

    private static final Locator RESULT_COUNT = Locator.of("Catalogue: result count",
            By.cssSelector("[data-testid='result-count']"));

    private static final Locator CART_LINK = Locator.of("Header: cart link",
            By.cssSelector("[data-testid='cart-link']"));

    private static final Locator CART_BADGE = Locator.of("Header: cart badge",
            By.cssSelector("[data-testid='cart-count']"));

    private static final Locator CURRENT_USER = Locator.of("Header: signed in user",
            By.cssSelector("[data-testid='current-user']"));

    @Override
    protected Locator pageMarker() {
        // The grid is hidden until the asynchronous render completes, so waiting
        // for it to be *visible* is the same as waiting for the catalogue.
        return GRID;
    }

    // ------------------------------------------------------------------ queries

    public List<String> productNames() {
        return $(PRODUCT_NAMES).allTexts();
    }

    public int productCount() {
        return $(PRODUCT_CARDS).count();
    }

    public List<BigDecimal> prices() {
        return $(PRODUCT_PRICES).allTexts().stream().map(Money::parse).toList();
    }

    public String resultCount() {
        return $(RESULT_COUNT).text();
    }

    public String signedInUser() {
        return $(CURRENT_USER).text();
    }

    public boolean spinnerIsGone() {
        return $(SPINNER).isAbsent(Duration.ofSeconds(5));
    }

    /** The badge is hidden while the cart is empty: absence means zero, not a failure. */
    public int cartBadgeCount() {
        if (!$(CART_BADGE).isVisibleWithin(Duration.ofSeconds(2))) {
            return 0;
        }
        return Integer.parseInt($(CART_BADGE).text());
    }

    public boolean isAddToCartEnabled(String productName) {
        return $(addToCartButtonFor(productName)).isEnabled();
    }

    public String stockLabelOf(String productName) {
        return $(Locator.dynamic("Catalogue: stock label of '%s'",
                "//article[@data-testid='product-card'][.//*[@data-testid='product-name']"
                        + "[normalize-space()='%s']]//*[@data-testid='product-stock']",
                productName)).text();
    }

    // ------------------------------------------------------------------ actions

    public InventoryPage filterByCategory(String category) {
        ReportManager.step("Filtering the catalogue by '%s'".formatted(category));
        $(CATEGORY_FILTER).selectByVisibleText(category);
        return this;
    }

    public InventoryPage sortBy(String order) {
        ReportManager.step("Sorting the catalogue by '%s'".formatted(order));
        $(SORT_ORDER).selectByVisibleText(order);
        return this;
    }

    public InventoryPage addToCart(String productName) {
        int before = cartBadgeCount();
        $(addToCartButtonFor(productName)).click();
        // Assert the side effect, not the click: the badge is the observable contract.
        waitUntil(() -> cartBadgeCount() > before,
                "cart badge to grow after adding '%s'".formatted(productName));
        return this;
    }

    public CartPage openCart() {
        $(CART_LINK).click();
        return new CartPage().waitUntilLoaded();
    }

    // ------------------------------------------------------------------ internals

    private Locator addToCartButtonFor(String productName) {
        return Locator.dynamic("Catalogue: add '%s' to cart",
                "//article[@data-testid='product-card'][.//*[@data-testid='product-name']"
                        + "[normalize-space()='%s']]//button[@data-testid='add-to-cart']",
                productName);
    }
}
