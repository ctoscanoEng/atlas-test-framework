package io.atlas.qa.bdd.steps;

import io.atlas.qa.bdd.support.ScenarioContext;
import io.atlas.qa.sandbox.pages.CartPage;
import io.atlas.qa.sandbox.pages.CheckoutReviewPage;
import io.atlas.qa.sandbox.pages.InventoryPage;
import io.atlas.qa.sandbox.pages.OrderConfirmationPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

/** Steps of the purchase feature. */
public class CheckoutSteps {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");

    private final ScenarioContext context;

    public CheckoutSteps(ScenarioContext context) {
        this.context = context;
    }

    @When("I add {string} to the cart")
    public void iAddToTheCart(String product) {
        context.put("catalogue", context.get("catalogue", InventoryPage.class).addToCart(product));
    }

    @Then("the cart badge shows {int} items")
    public void theCartBadgeShows(int expected) {
        assertThat(context.get("catalogue", InventoryPage.class).cartBadgeCount()).isEqualTo(expected);
    }

    @When("I open the cart")
    public void iOpenTheCart() {
        context.put("cart", context.get("catalogue", InventoryPage.class).openCart());
    }

    @Then("the total is the subtotal plus {int} percent of tax")
    public void theTotalIsSubtotalPlusTax(int percent) {
        CartPage cart = context.get("cart", CartPage.class);
        BigDecimal expectedTax = cart.subtotal()
                .multiply(BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100)))
                .setScale(2, RoundingMode.HALF_UP);

        assertThat(TAX_RATE).isEqualByComparingTo(BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100)));
        assertThat(cart.tax()).isEqualByComparingTo(expectedTax);
        assertThat(cart.total()).isEqualByComparingTo(cart.subtotal().add(expectedTax));
    }

    @Then("the cart contains {int} line")
    public void theCartContainsLines(int expected) {
        assertThat(context.get("cart", CartPage.class).lineCount()).isEqualTo(expected);
    }

    @Then("the quantity of {string} is {int}")
    public void theQuantityOfIs(String product, int expected) {
        assertThat(context.get("cart", CartPage.class).quantityOf(product)).isEqualTo(expected);
    }

    @When("I complete the checkout with my delivery details")
    public void iCompleteTheCheckout() {
        CartPage cart = context.get("cart", CartPage.class);
        CheckoutReviewPage review = cart.proceedToCheckout()
                .fill(context.customer())
                .submit();

        context.put("reviewedAmount", review.total());
        context.put("confirmation", review.placeOrder());
    }

    @Then("the order is confirmed")
    public void theOrderIsConfirmed() {
        OrderConfirmationPage confirmation = context.get("confirmation", OrderConfirmationPage.class);

        assertThat(confirmation.heading()).contains("your order is confirmed");
        assertThat(confirmation.orderReference()).matches("ATL-\\d{4}-\\d{6}");
    }

    @Then("the amount charged is the amount I reviewed")
    public void theAmountChargedIsTheAmountReviewed() {
        assertThat(context.get("confirmation", OrderConfirmationPage.class).amountCharged())
                .isEqualByComparingTo(context.get("reviewedAmount", BigDecimal.class));
    }

    @When("I try to check out")
    public void iTryToCheckOut() {
        context.put("cart", context.get("cart", CartPage.class).attemptCheckout());
    }

    @Then("the cart shows {string}")
    public void theCartShows(String message) {
        CartPage cart = context.get("cart", CartPage.class);
        assertThat(cart.isErrorDisplayed()).isTrue();
        assertThat(cart.errorMessage()).contains(message);
    }
}
