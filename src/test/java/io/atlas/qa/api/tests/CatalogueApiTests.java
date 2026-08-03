package io.atlas.qa.api.tests;

import io.atlas.qa.base.BaseApiTest;
import io.atlas.qa.core.sandbox.SandboxUsers;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract of the service behind the store.
 *
 * <p>These tests run in a few hundred milliseconds and cover the rules that a UI
 * test would spend thirty seconds proving through a browser. The pyramid is not
 * a slogan here: the browser suite checks what the user sees, this suite checks
 * what the system promises.
 */
public class CatalogueApiTests extends BaseApiTest {

    @Test(groups = {"smoke", "api"}, description = "The service reports itself as healthy")
    public void healthEndpointIsUp() {
        Response response = api.get("/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("status")).isEqualTo("UP");
    }

    @Test(groups = {"smoke", "api"},
            description = "The catalogue payload honours its JSON schema, field by field")
    public void catalogueHonoursItsSchema() {
        Response response = api.get("/products");

        assertThat(response.statusCode()).isEqualTo(200);
        response.then().assertThat().body(matchesJsonSchemaInClasspath("schemas/products-schema.json"));

        List<Map<String, Object>> products = response.jsonPath().getList("products");
        assertThat(products).hasSize(8);
        assertThat(response.jsonPath().getString("currency")).isEqualTo("EUR");
    }

    @Test(groups = {"regression", "api"},
            description = "Every product exposed by the list is retrievable on its own")
    public void everyProductIsRetrievableById() {
        List<String> identifiers = api.get("/products").jsonPath().getList("products.id");

        assertThat(identifiers).isNotEmpty();
        identifiers.forEach(id -> {
            Response product = api.get("/products/{id}", id);
            assertThat(product.statusCode()).as("status for %s", id).isEqualTo(200);
            assertThat(product.jsonPath().getString("id")).isEqualTo(id);
        });
    }

    @Test(groups = {"regression", "api"},
            description = "An unknown product returns 404 with a readable message, not a stack trace")
    public void unknownProductReturnsNotFound() {
        Response response = api.get("/products/{id}", "ATL-999");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.jsonPath().getString("error")).contains("does not exist");
    }

    @Test(groups = {"smoke", "api"}, description = "Valid credentials return a token and the account roles")
    public void validCredentialsReturnAToken() {
        Response response = api.post("/auth/login",
                Map.of("username", SandboxUsers.STANDARD, "password", SandboxUsers.PASSWORD));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("token")).startsWith("atlas.");
        assertThat(response.jsonPath().getList("roles", String.class)).contains("CHECKOUT");
    }

    @Test(groups = {"regression", "api"},
            description = "A wrong password is refused without leaking whether the account exists")
    public void wrongPasswordIsRefused() {
        Response response = api.post("/auth/login",
                Map.of("username", SandboxUsers.STANDARD, "password", "wrong"));

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.jsonPath().getString("error")).isEqualTo("Invalid credentials");
        assertThat(response.body().asString())
                .as("the response must not disclose the existence of the account")
                .doesNotContain("password");
    }

    @Test(groups = {"regression", "api"},
            description = "A locked account is refused with its own status code")
    public void lockedAccountIsRefusedWithADedicatedStatus() {
        Response response = api.post("/auth/login",
                Map.of("username", SandboxUsers.LOCKED, "password", SandboxUsers.PASSWORD));

        assertThat(response.statusCode()).isEqualTo(423);
        assertThat(response.jsonPath().getString("error")).contains("locked");
    }

    @Test(groups = {"regression", "api"},
            description = "A read-only account is authenticated but carries no checkout permission")
    public void readOnlyAccountHasNoCheckoutPermission() {
        Response response = api.post("/auth/login",
                Map.of("username", SandboxUsers.READONLY, "password", SandboxUsers.PASSWORD));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("roles", String.class))
                .contains("CUSTOMER")
                .doesNotContain("CHECKOUT");
    }
}
