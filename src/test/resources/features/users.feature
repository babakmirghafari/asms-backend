Feature: User Management
  As an ASMS administrator
  I want to manage user lifecycle
  So that I can control access within my organization

  Background:
    Given the service is running

  Scenario: Create a new user
    When I create a user with valid details
    Then the response status is 201
    And the response contains field "id"

  # Wizard endpoint not yet in contract — keeping pending until implemented
  @Pending
  Scenario: Complete 8-step user creation wizard
    When I start the user creation wizard with step "BASIC_INFO" and payload:
      """
      {"firstName":"John","lastName":"Doe","email":"john.doe@example.com","username":"jdoe"}
      """
    Then the wizard response status is 200
    And the wizard step response contains field "wizardSessionId"
    And the wizard step response contains field "nextStep"

  Scenario: Retrieve an existing user
    Given a user exists
    When I request the user by id
    Then the response status is 200
    And the response contains field "id"

  Scenario: Update user status to locked
    Given a user exists with status "ACTIVE"
    When I update the user status to "LOCKED"
    Then the response status is 200
    And the response contains field "id"
