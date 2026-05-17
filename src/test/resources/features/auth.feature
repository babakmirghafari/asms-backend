Feature: Authentication Flows
  As a user
  I want to authenticate with ASMS
  So that I can access my organization's resources

  Background:
    Given the service is running

  Scenario: Standard login initiates MFA challenge
    Given a user with username "mfa-bdd@example.com" exists with status "ACTIVE"
    When I login with username "mfa-bdd@example.com" and password "ValidPass123!"
    Then the login response status is 200
    And the response requires MFA verification

  Scenario: First login forces password change
    Given a user with username "firstlogin-bdd@example.com" exists with status "ACTIVE"
    And the user was issued a temporary password "TempPass456!"
    When I login with username "firstlogin-bdd@example.com" and password "TempPass456!"
    Then the login response status is 200
    And the response requires password change

  Scenario: Account locked after exceeding failed login threshold
    Given a user with username "lockout-bdd@example.com" exists with status "ACTIVE"
    And the user has failed login 3 times
    When I login with username "lockout-bdd@example.com" and password "WrongPassword"
    Then the account is locked

  # Station policy enforcement in login not yet implemented in AuthService
  @Pending
  Scenario: Login denied due to station policy violation
    Given a user with username "station@example.com" exists with status "ACTIVE"
    When I login with username "station@example.com" and password "ValidPass123!"
    Then the login response status is 403
