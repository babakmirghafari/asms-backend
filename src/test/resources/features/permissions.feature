Feature: Permission Management
  As an ASMS administrator
  I want to manage the permission catalog
  So that I can control what actions applications can perform

  Background:
    Given the service is running

  # TODO: implement after permission domain entity and repository are wired
  @Pending
  Scenario: Create a new permission
    When I create a permission with valid details
    Then the response status is 201
    And the response contains field "id"

  # TODO: implement after permission domain entity and repository are wired
  @Pending
  Scenario: Retrieve an existing permission
    Given a permission exists
    When I request the permission by id
    Then the response status is 200
    And the response contains field "id"

  # AC-13: simulate permission — v2.0.0 path and DTO
  # Path changed from POST /access-control/simulate to POST /permissions/simulate
  # Request DTO changed from {resource, action} to {permissionName}
  @Pending
  Scenario: Simulate permission evaluation — user is GRANTED access
    When I simulate permission "hr.employee.read" for a user in my organization
    Then the response status is 200
    And the response contains field "decision"
    And the field "decision" equals "GRANTED"

  @Pending
  Scenario: Simulate permission evaluation — user is DENIED access
    When I simulate permission "hr.payroll.admin" for a user without that permission
    Then the response status is 200
    And the response contains field "decision"
    And the field "decision" equals "DENIED"
