Feature: Two-Step Permission CSV Import
  As an ASMS administrator
  I want to import permissions from a CSV file with a validate-then-commit workflow
  So that I can review import issues before committing any changes to the database

  Background:
    Given the service is running

  Scenario: Validate a CSV file and commit the import
    When I upload a valid permissions CSV for my organization
    Then the response status is 200
    And the response contains field "importId"
    And the response contains field "status"
    And the field "status" equals "READY"
    When I commit the import using the returned importId
    Then the response status is 201
    And the response contains field "committed"
    And the response contains field "importId"

  Scenario: Validate a CSV file with errors — import is BLOCKED
    When I upload a permissions CSV containing invalid rows for my organization
    Then the response status is 200
    And the response contains field "importId"
    And the field "status" equals "BLOCKED"
    And the response contains field "issues"

  Scenario: Cannot commit a BLOCKED import session
    Given a permission import session exists with status "BLOCKED"
    When I commit the import using the BLOCKED importId
    Then the response status is 409

  Scenario: Retrieve the import report after committing
    Given a permission import session has been committed
    When I request the import report by importId
    Then the response status is 200
    And the response contains field "importId"
    And the response contains field "phase"
    And the field "phase" equals "COMMITTED"

  Scenario: Cannot commit an expired import session
    Given a permission import session exists that has expired
    When I commit the import using the expired importId
    Then the response status is 400
