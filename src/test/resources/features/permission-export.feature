Feature: Permission Catalog CSV Export
  As an ASMS administrator
  I want to export the permissions catalog as a CSV file
  So that I can audit or bulk-manage permissions outside the application

  Background:
    Given the service is running

  Scenario: Export returns CSV with header when no permissions exist
    Given I have a new organization with no permissions
    When I request the permissions CSV export for that organization
    Then the response status is 200
    And the response content-type is "text/csv"
    And the response body contains "id,name,resource,action,status"

  Scenario: Export includes a permission row when permissions exist
    Given I have an organization with an ACTIVE permission "hr.employee.read"
    When I request the permissions CSV export for that organization
    Then the response status is 200
    And the response body contains "hr.employee.read"

  Scenario: Export filtered by status returns only matching rows
    Given I have an organization with a DRAFT permission "hr.payroll.write"
    And the same organization has an ACTIVE permission "hr.payroll.read"
    When I request the permissions CSV export filtered by status "ACTIVE"
    Then the response status is 200
    And the response body contains "hr.payroll.read"
    And the response body does not contain "hr.payroll.write"
