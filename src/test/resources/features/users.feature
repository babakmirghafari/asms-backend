Feature: User Management
  As an ASMS administrator
  I want to manage user lifecycle
  So that I can control access within my organization

  Background:
    Given the service is running

  # TODO: implement after user domain entity and repository are wired
  @Pending
  Scenario: Create a new user
    When I create a user with valid details
    Then the response status is 201
    And the response contains field "id"

  # TODO: implement after user domain entity and repository are wired
  @Pending
  Scenario: Retrieve an existing user
    Given a user exists
    When I request the user by id
    Then the response status is 200
    And the response contains field "id"

  # TODO: implement after user domain entity and repository are wired
  @Pending
  Scenario: Update user status to locked
    Given a user exists with status "ACTIVE"
    When I update the user status to "LOCKED"
    Then the response status is 200
    And the response contains field "id"
