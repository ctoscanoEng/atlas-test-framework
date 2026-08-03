package io.atlas.qa.sandbox.tests;

import io.atlas.qa.base.BaseWebTest;
import io.atlas.qa.core.listener.NoRetry;
import io.atlas.qa.core.sandbox.SandboxUsers;
import io.atlas.qa.domain.Customer;
import io.atlas.qa.sandbox.pages.CartPage;
import io.atlas.qa.sandbox.pages.CheckoutDetailsPage;
import io.atlas.qa.sandbox.pages.CheckoutReviewPage;
import io.atlas.qa.sandbox.pages.LoginPage;
import io.atlas.qa.sandbox.pages.OrderConfirmationPage;
import io.atlas.qa.support.Money;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end purchase journey — the scenario that actually pays the bills.
 *
 * <p>The arithmetic is verified rather than eyeballed: the test recomputes the
 * tax and the total from the line prices and compares with what the application
 * renders at every step. A checkout suite that only asserts "the confirmation
 * page is displayed" would pass while charging the customer the wrong amount.
 */
public class CheckoutJourneyTests extends BaseWebTest {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");

    @Test(groups = {"smoke", "checkout"},
            description = "A customer buys two products and is charged the amount shown in the cart")
    public void customerCompletesAPurchase() {
        CartPage cart = LoginPage.open()
                .signInAs(SandboxUsers.STANDARD, SandboxUsers.PASSWORD)
                .addToCart("Summit Down Jacket")
                .addToCart("Basecamp Headlamp 600")
                .openCart();

        BigDecimal subtotal = cart.subtotal();
        BigDecimal expectedTax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);

        assertThat(subtotal).isEqualByComparingTo(Money.of("313.90"));
        assertThat(cart.tax()).as("tax computed on the subtotal").isEqualByComparingTo(expectedTax);
        assertThat(cart.total()).as("total charged to the customer")
                .isEqualByComparingTo(subtotal.add(expectedTax));

        Customer customer = Customer.random();
        CheckoutReviewPage review = cart.proceedToCheckout()
                .fill(customer)
                .submit();

        assertThat(review.lineCount()).isEqualTo(2);
        assertThat(review.deliverySummary())
                .contains(customer.fullName())
                .contains(customer.postcode())
                .contains(customer.email());
        assertThat(review.total())
                .as("the total must not change between the cart and the review step")
                .isEqualByComparingTo(cart.total());

        BigDecimal amountToPay = review.total();
        OrderConfirmationPage confirmation = review.placeOrder();

        assertThat(confirmation.heading()).contains("your order is confirmed");
        assertThat(confirmation.orderReference())
                .as("order reference format")
                .matches("ATL-\\d{4}-\\d{6}");
        assertThat(confirmation.itemCount()).isEqualTo(2);
        assertThat(confirmation.amountCharged())
                .as("amount charged must equal the amount reviewed")
                .isEqualByComparingTo(amountToPay);
    }

    @Test(groups = {"regression", "checkout"},
            description = "The checkout form lists every mandatory field left empty")
    public void mandatoryFieldsAreEnforced() {
        CheckoutDetailsPage details = LoginPage.open()
                .signInAs(SandboxUsers.STANDARD, SandboxUsers.PASSWORD)
                .addToCart("Nomad Merino Base Layer")
                .openCart()
                .proceedToCheckout()
                .fillPartially("Giulia", "", "", "")
                .submitExpectingRejection();

        assertThat(details.errorMessage())
                .contains("last name")
                .contains("email")
                .contains("postcode")
                .doesNotContain("first name");
    }

    @Test(groups = {"regression", "checkout"},
            description = "A malformed email address is rejected with the offending value quoted")
    public void malformedEmailIsRejected() {
        CheckoutDetailsPage details = LoginPage.open()
                .signInAs(SandboxUsers.STANDARD, SandboxUsers.PASSWORD)
                .addToCart("Nomad Merino Base Layer")
                .openCart()
                .proceedToCheckout()
                .fill(Customer.random().withEmail("giulia.rossi.atlas.test"))
                .submitExpectingRejection();

        assertThat(details.errorMessage()).contains("is not a valid email address");
    }

    @NoRetry // proves a permission rule: re-running it would only hide a real regression
    @Test(groups = {"regression", "checkout", "authorisation"},
            description = "A read-only account can fill a cart but cannot place an order")
    public void readOnlyAccountCannotPlaceAnOrder() {
        CartPage cart = LoginPage.open()
                .signInAs(SandboxUsers.READONLY, SandboxUsers.PASSWORD)
                .addToCart("Ridge GPS Watch")
                .openCart()
                .attemptCheckout();

        assertThat(cart.isErrorDisplayed()).isTrue();
        assertThat(cart.errorMessage()).contains("not allowed to place orders");
        assertThat(cart.currentUrl())
                .as("the read-only user must not reach the checkout flow")
                .endsWith("cart.html");
    }

    @Test(groups = {"regression", "checkout"},
            description = "Removing the last line empties the cart and disables the checkout")
    public void emptyingTheCartDisablesTheCheckout() {
        CartPage cart = LoginPage.open()
                .signInAs(SandboxUsers.STANDARD, SandboxUsers.PASSWORD)
                .addToCart("Vertex Climbing Harness")
                .openCart()
                .removeLine("Vertex Climbing Harness");

        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.isCheckoutEnabled()).isFalse();
    }
}
