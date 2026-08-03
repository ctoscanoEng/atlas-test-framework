package io.atlas.qa.e2e.pages;

import io.atlas.qa.core.element.Locator;
import io.atlas.qa.core.page.BasePage;
import io.atlas.qa.domain.Customer;
import org.openqa.selenium.By;

import java.util.List;

/**
 * The whole purchase journey of the third-party demo store, modelled as one
 * object because the flow is short and the pages share a header.
 */
public final class SauceStorePage extends BasePage {

    private static final Locator INVENTORY = Locator.of("SauceDemo: inventory list",
            By.cssSelector(".inventory_list"));

    private static final Locator ITEM_NAMES = Locator.of("SauceDemo: product names",
            By.cssSelector(".inventory_item_name"));

    private static final Locator CART_LINK = Locator.of("SauceDemo: cart link",
            By.cssSelector(".shopping_cart_link"));

    private static final Locator CART_BADGE = Locator.of("SauceDemo: cart badge",
            By.cssSelector(".shopping_cart_badge"));

    private static final Locator CHECKOUT = Locator.named("SauceDemo: checkout")
            .by(By.id("checkout"))
            .orBy(By.cssSelector("[data-test='checkout']"))
            .build();

    private static final Locator FIRST_NAME = Locator.of("SauceDemo: first name", By.id("first-name"));
    private static final Locator LAST_NAME = Locator.of("SauceDemo: last name", By.id("last-name"));
    private static final Locator POSTCODE = Locator.of("SauceDemo: postal code", By.id("postal-code"));
    private static final Locator CONTINUE = Locator.of("SauceDemo: continue", By.id("continue"));
    private static final Locator FINISH = Locator.of("SauceDemo: finish", By.id("finish"));
    private static final Locator TOTAL = Locator.of("SauceDemo: total", By.cssSelector(".summary_total_label"));
    private static final Locator CONFIRMATION = Locator.of("SauceDemo: confirmation",
            By.cssSelector(".complete-header"));

    @Override
    protected Locator pageMarker() {
        return INVENTORY;
    }

    public List<String> productNames() {
        return $(ITEM_NAMES).allTexts();
    }

    public SauceStorePage addToCart(String productName) {
        $(Locator.dynamic("SauceDemo: add '%s' to cart",
                "//div[@class='inventory_item'][.//div[@class='inventory_item_name '][text()='%s']]"
                        + "//button[contains(@class,'btn_inventory')]",
                productName)).click();
        return this;
    }

    public int cartBadgeCount() {
        return Integer.parseInt($(CART_BADGE).text());
    }

    public SauceStorePage openCart() {
        $(CART_LINK).click();
        return this;
    }

    public SauceStorePage checkout(Customer customer) {
        $(CHECKOUT).click();
        $(FIRST_NAME).type(customer.firstName());
        $(LAST_NAME).type(customer.lastName());
        $(POSTCODE).type(customer.postcode());
        $(CONTINUE).click();
        return this;
    }

    public String summaryTotal() {
        return $(TOTAL).text();
    }

    public String placeOrder() {
        $(FINISH).click();
        return $(CONFIRMATION).text();
    }
}
