Feature: Alert Lifecycle Management
  As an ASMS administrator
  I want to manage security alert lifecycle
  So that I can track, escalate, and resolve security incidents

  Background:
    Given the service is running

  # AC-7: resolveAlert
  @Pending
  Scenario: Resolve an open security alert
    Given a security alert exists with status "OPEN"
    When I resolve the alert with note "Root cause identified and remediated"
    Then the response status is 200
    And the response contains field "id"

  # AC-8: escalateAlert
  @Pending
  Scenario: Escalate a security alert to CISO
    Given a security alert exists with status "OPEN"
    When I escalate the alert with reason "Suspected targeted attack" and target "CISO"
    Then the response status is 200
    And the response contains field "id"

  # AC-8: escalateAlert — negative path
  @Pending
  Scenario: Cannot escalate an already-resolved alert
    Given a security alert exists with status "RESOLVED"
    When I escalate the alert with reason "Late escalation attempt" and target "CISO"
    Then the response status is 409

  # v2.0.0 additive: list alerts includes riskScore and riskLevel
  @Pending
  Scenario: List alerts returns riskScore and riskLevel fields
    When I list alerts for my organization
    Then the response status is 200
    And the response contains field "content"
