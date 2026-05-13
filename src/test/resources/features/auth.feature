Feature: Authentication Flows
  As a user
  I want to authenticate with ASMS
  So that I can access my organization's resources

  Background:
    Given the service is running

  # TODO: implement after auth endpoint is wired
  # Runtime Flow 1: Standard login with MFA
  @Pending
  Scenario: Standard login initiates MFA challenge
    When I login with username "testuser@example.com" and password "ValidPass123!"
    Then the login response status is 200
    And the response requires MFA verification

  # TODO: implement after temp-password / first-login logic is wired
  # Runtime Flow 2: First login with temporary password
  @Pending
  Scenario: First login forces password change
    Given a user with username "firstlogin@example.com" exists with status "ACTIVE"
    And the user was issued a temporary password "TempPass456!"
    When I login with username "firstlogin@example.com" and password "TempPass456!"
    Then the login response status is 200
    And the response requires password change

  # TODO: implement after account lockout logic is wired
  # Runtime Flow 3: Account lockout after failed attempts
  @Pending
  Scenario: Account locked after exceeding failed login threshold
    Given a user with username "lockout@example.com" exists with status "ACTIVE"
    And the user has failed login 3 times
    When I login with username "lockout@example.com" and password "WrongPassword"
    Then the account is locked

  # TODO: implement after station policy logic is wired
  # Runtime Flow 4: Station policy access denied
  @Pending
  Scenario: Login denied due to station policy violation
    Given a user with username "station@example.com" exists with status "ACTIVE"
    When I login with username "station@example.com" and password "ValidPass123!"
    Then the login response status is 403
