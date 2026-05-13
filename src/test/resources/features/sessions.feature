Feature: Session Management
  As an ASMS administrator
  I want to monitor and revoke user sessions
  So that I can maintain security across the organization

  Background:
    Given the service is running

  # TODO: implement after session domain entity and repository are wired
  # Runtime Flow 9: Session revocation
  @Pending
  Scenario: Revoke an active session
    Given a session exists
    When I revoke the session by id
    Then the response status is 204

  # TODO: implement after session domain entity and repository are wired
  @Pending
  Scenario: List active sessions for an organization
    When I list sessions for my organization
    Then the response status is 200
    And the response contains field "content"
