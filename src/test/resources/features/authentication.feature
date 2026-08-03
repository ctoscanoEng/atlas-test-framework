@authentication
Feature: Access to the Atlas Outdoor back office
  As the store manager
  I want the back office to accept only valid, unlocked accounts
  So that stock and orders cannot be touched by the wrong people

  Background:
    Given the sign-in page is open

  @smoke
  Scenario: A store manager signs in and reaches the catalogue
    When I sign in as "standard_user" with the password "atlas_secret"
    Then the catalogue is displayed
    And the header shows that "standard_user" is signed in

  @regression
  Scenario Outline: The application refuses <scenario>
    When I sign in as "<username>" with the password "<password>"
    Then the sign-in page shows "<message>"
    And I am still on the sign-in page

    Examples:
      | scenario                       | username      | password         | message                                       |
      | a wrong password               | standard_user | not_the_password | Invalid credentials                           |
      | an account that does not exist | ghost_user    | atlas_secret     | Invalid credentials                           |
      | a locked account               | locked_user   | atlas_secret     | Account is locked, contact your administrator |
      | an empty username              |               | atlas_secret     | Username and password are both required.      |
