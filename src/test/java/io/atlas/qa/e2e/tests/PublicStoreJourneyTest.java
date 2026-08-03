package io.atlas.qa.e2e.tests;

import io.atlas.qa.base.BaseWebTest;
import io.atlas.qa.domain.Customer;
import io.atlas.qa.e2e.pages.SauceLoginPage;
import io.atlas.qa.e2e.pages.SauceStorePage;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The same framework, pointed at an application it does not host.
 *
 * <p>Run with {@code ./mvnw test -Pe2e}: the {@code staging} overlay changes the
 * base URL, the credentials and the timeouts. Nothing in {@code src/main/java}
 * knows this application exists — which is the actual proof that the core is a
 * framework and not a pile of helpers glued to one website.
 *
 * <p>Kept out of the default pipeline on purpose: a public demo site is a
 * dependency nobody on the team controls.
 */
public class PublicStoreJourneyTest extends BaseWebTest {

    @Test(groups = {"e2e"},
            description = "A customer signs in to a public demo store and completes a purchase")
    public void customerCompletesAPurchaseOnAPublicStore() {
        SauceStorePage store = SauceLoginPage.open()
                .signInWithConfiguredCredentials()
                .addToCart("Sauce Labs Backpack")
                .addToCart("Sauce Labs Bike Light");

        assertThat(store.cartBadgeCount()).isEqualTo(2);

        String confirmation = store.openCart()
                .checkout(Customer.random())
                .placeOrder();

        assertThat(confirmation).containsIgnoringCase("Thank you for your order");
    }
}
