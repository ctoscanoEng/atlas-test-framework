package io.atlas.qa.domain;

import io.atlas.qa.core.data.FakeData;

/**
 * A buyer of the store.
 *
 * <p>A record rather than a bag of strings: {@code checkout(Customer)} is
 * impossible to call with the postcode in the email slot, and Jackson maps the
 * JSON fixtures onto it without a line of glue code.
 */
public record Customer(
        String firstName,
        String lastName,
        String email,
        String address,
        String city,
        String postcode) {

    public String fullName() {
        return "%s %s".formatted(firstName, lastName);
    }

    /** Fresh, reproducible data — see {@link FakeData} for how the seed is fixed. */
    public static Customer random() {
        return new Customer(
                FakeData.firstName(),
                FakeData.lastName(),
                FakeData.email(),
                FakeData.streetAddress(),
                FakeData.city(),
                FakeData.postcode());
    }

    public Customer withEmail(String other) {
        return new Customer(firstName, lastName, other, address, city, postcode);
    }
}
