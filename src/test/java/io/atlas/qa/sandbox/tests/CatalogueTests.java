package io.atlas.qa.sandbox.tests;

import io.atlas.qa.base.BaseWebTest;
import io.atlas.qa.core.sandbox.SandboxUsers;
import io.atlas.qa.sandbox.pages.InventoryPage;
import io.atlas.qa.sandbox.pages.LoginPage;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Browsing, filtering and sorting the catalogue. */
public class CatalogueTests extends BaseWebTest {

    private InventoryPage signIn() {
        return LoginPage.open().signInAs(SandboxUsers.STANDARD, SandboxUsers.PASSWORD);
    }

    @Test(groups = {"smoke", "catalogue"},
            description = "The catalogue finishes loading and renders the whole product set")
    public void catalogueFinishesLoading() {
        InventoryPage catalogue = signIn();

        assertThat(catalogue.spinnerIsGone())
                .as("the loading indicator must disappear once the products arrive")
                .isTrue();
        assertThat(catalogue.productCount()).isEqualTo(8);
        assertThat(catalogue.resultCount()).isEqualTo("8 of 8 products");
    }

    @Test(groups = {"regression", "catalogue"},
            description = "Filtering by category narrows the grid to that category only")
    public void filteringByCategoryNarrowsTheGrid() {
        InventoryPage catalogue = signIn().filterByCategory("Equipment");

        assertThat(catalogue.productCount()).isEqualTo(3);
        assertThat(catalogue.productNames())
                .containsExactlyInAnyOrder(
                        "Granite Trekking Poles",
                        "Vertex Climbing Harness",
                        "Drylite Sleeping Bag -5°C");
    }

    @Test(groups = {"regression", "catalogue"},
            description = "Sorting by price descending really orders the rendered prices")
    public void sortingByPriceIsApplied() {
        List<BigDecimal> prices = signIn().sortBy("Price (high → low)").prices();

        assertThat(prices).isSortedAccordingTo((left, right) -> right.compareTo(left));
        assertThat(prices.get(0)).isEqualByComparingTo("419.00");
    }

    @Test(groups = {"regression", "catalogue"},
            description = "A product with no stock cannot be added to the cart")
    public void outOfStockProductCannotBeAdded() {
        InventoryPage catalogue = signIn();

        assertThat(catalogue.stockLabelOf("Granite Trekking Poles")).isEqualTo("Out of stock");
        assertThat(catalogue.isAddToCartEnabled("Granite Trekking Poles"))
                .as("the add-to-cart button of an unavailable product")
                .isFalse();
    }

    @Test(groups = {"smoke", "catalogue"},
            description = "Adding products updates the cart badge and the cart itself")
    public void addingProductsUpdatesTheCart() {
        InventoryPage catalogue = signIn()
                .addToCart("Summit Down Jacket")
                .addToCart("Basecamp Headlamp 600")
                .addToCart("Summit Down Jacket");

        assertThat(catalogue.cartBadgeCount()).as("total quantity in the badge").isEqualTo(3);

        var cart = catalogue.openCart();
        assertThat(cart.lineCount()).as("distinct lines in the cart").isEqualTo(2);
        assertThat(cart.quantityOf("Summit Down Jacket")).isEqualTo(2);
        assertThat(cart.lineNames()).containsExactlyInAnyOrder("Summit Down Jacket", "Basecamp Headlamp 600");
    }
}
