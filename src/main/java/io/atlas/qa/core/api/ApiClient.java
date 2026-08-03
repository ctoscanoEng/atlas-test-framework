package io.atlas.qa.core.api;

import io.atlas.qa.core.config.AtlasConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Thin, immutable client over REST Assured.
 *
 * <h2>Why the framework has an API layer at all</h2>
 * Two reasons, both about the UI suite:
 * <ul>
 *   <li><b>speed</b> — logging in through the API and injecting the session
 *       costs ~50 ms against ~3 s of form typing, multiplied by every test;</li>
 *   <li><b>isolation</b> — preconditions (a user, an order, a full cart) are
 *       created through the API, so a UI test about checkout does not fail
 *       because the registration page is broken.</li>
 * </ul>
 * Each {@code withX} method returns a new instance: sharing one mutable
 * specification between parallel threads is a classic source of cross-talk
 * between tests.
 */
public final class ApiClient {

    private final RequestSpecification specification;

    private ApiClient(RequestSpecification specification) {
        this.specification = specification;
    }

    /** Client pointed at the API of the application under test. */
    public static ApiClient forApplication() {
        return forBaseUri(AtlasConfig.apiBaseUrl());
    }

    public static ApiClient forBaseUri(String baseUri) {
        return new ApiClient(new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new ApiTraceFilter())
                .build());
    }

    public ApiClient withBearerToken(String token) {
        return new ApiClient(new RequestSpecBuilder()
                .addRequestSpecification(specification)
                .addHeader("Authorization", "Bearer " + token)
                .build());
    }

    public ApiClient withHeaders(Map<String, String> headers) {
        return new ApiClient(new RequestSpecBuilder()
                .addRequestSpecification(specification)
                .addHeaders(headers)
                .build());
    }

    // ------------------------------------------------------------------ verbs

    public Response get(String path, Object... pathParameters) {
        return given().spec(specification).when().get(path, pathParameters).andReturn();
    }

    public Response post(String path, Object body) {
        return given().spec(specification).body(body).when().post(path).andReturn();
    }

    public Response put(String path, Object body) {
        return given().spec(specification).body(body).when().put(path).andReturn();
    }

    public Response patch(String path, Object body) {
        return given().spec(specification).body(body).when().patch(path).andReturn();
    }

    public Response delete(String path, Object... pathParameters) {
        return given().spec(specification).when().delete(path, pathParameters).andReturn();
    }

    public RequestSpecification specification() {
        return specification;
    }
}
