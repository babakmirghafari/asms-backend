Feature: Permission Management
  As an ASMS administrator
  I want to manage the permission catalog
  So that I can control what actions applications can perform

  Background:
    Given the service is running

  Scenario: Create a new permission
    When I create a permission with valid details
    Then the response status is 201
    And the response contains field "id"

  Scenario: Upload a CSV file for permission import
    When I upload a valid permissions CSV for my organization
    Then the response status is 200
    And the response contains field "importId"
    And the response contains field "status"

  Scenario: Retrieve an existing permission
    Given a permission exists
    When I request the permission by id
    Then the response status is 200
    And the response contains field "id"

  Scenario: Simulate permission evaluation — user is GRANTED access
    When I simulate permission "hr.employee.read" for a user in my organization
    Then the response status is 200
    And the response contains field "decision"
    And the field "decision" equals "GRANTED"

  Scenario: Simulate permission evaluation — user is DENIED access
    When I simulate permission "hr.payroll.admin" for a user without that permission
    Then the response status is 200
    And the response contains field "decision"
    And the field "decision" equals "DENIED"
