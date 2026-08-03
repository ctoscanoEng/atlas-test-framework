@checkout
Feature: Buying products from the catalogue
  As a customer of the store
  I want the amount I review to be the amount I am charged
  So that I can trust the checkout

  Background:
    Given I am signed in as "standard_user"

  @smoke
  Scenario: The amount charged is the amount reviewed
    When I add "Summit Down Jacket" to the cart
    And I add "Basecamp Headlamp 600" to the cart
    Then the cart badge shows 2 items
    When I open the cart
    Then the total is the subtotal plus 8 percent of tax
    When I complete the checkout with my delivery details
    Then the order is confirmed
    And the amount charged is the amount I reviewed

  @regression
  Scenario: Quantities add up instead of creating duplicate lines
    When I add "Nomad Merino Base Layer" to the cart
    And I add "Nomad Merino Base Layer" to the cart
    And I open the cart
    Then the cart contains 1 line
    And the quantity of "Nomad Merino Base Layer" is 2

  @regression @authorisation
  Scenario: A read-only account cannot place an order
    Given I am signed in as "readonly_user"
    When I add "Ridge GPS Watch" to the cart
    And I open the cart
    And I try to check out
    Then the cart shows "not allowed to place orders"
