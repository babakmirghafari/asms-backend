Feature: Application Health

  Background:
    Given the service is running

  Scenario: Health endpoint returns UP
    When I request the health endpoint
    Then the response status is 200
    And the health status is UP
