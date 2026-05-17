Feature: Permission Lifecycle Management
  As an ASMS administrator
  I want to manage permission lifecycle status
  So that I can control which permissions are available for assignment

  Background:
    Given the service is running

  Scenario: Activate a draft permission
    Given a permission exists with status "DRAFT"
    When I update the permission status to "ACTIVE" with reason "Permission approved for use"
    Then the response status is 200
    And the response contains field "id"

  Scenario: Deprecate an active permission
    Given a permission exists with status "ACTIVE"
    When I update the permission status to "DEPRECATED" with reason "Permission superseded"
    Then the response status is 200
    And the response contains field "id"

  Scenario: Cannot transition a deprecated permission back to active
    Given a permission exists with status "DEPRECATED"
    When I update the permission status to "ACTIVE" with reason "Invalid reversal"
    Then the response status is 400
