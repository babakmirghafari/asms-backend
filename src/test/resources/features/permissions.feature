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
