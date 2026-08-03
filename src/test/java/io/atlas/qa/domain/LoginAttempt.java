package io.atlas.qa.domain;

/**
 * One row of the negative-login data set.
 *
 * @param scenario        what the row proves, used as the name of the test in the report
 * @param username        value typed in the username field
 * @param password        value typed in the password field
 * @param expectedMessage message the application must show back to the user
 */
public record LoginAttempt(String scenario, String username, String password, String expectedMessage) {

    @Override
    public String toString() {
        return scenario;
    }
}
