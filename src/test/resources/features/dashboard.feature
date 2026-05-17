Feature: Dashboard KPI Summary
  As an ASMS administrator
  I want to see a dashboard summary of key security metrics
  So that I can monitor the security posture of my organization at a glance

  Background:
    Given the service is running

  Scenario: Dashboard summary returns all KPI fields
    When I request the dashboard summary
    Then the response status is 200
    And the response contains field "openAlertsBySeverity"
    And the response contains field "activeSessions"
    And the response contains field "totalUsers"
    And the response contains field "recentActivityCount"
    And the response contains field "generatedAt"

  Scenario: Dashboard openAlertsBySeverity contains all severity bands
    When I request the dashboard summary
    Then the response status is 200
    And the field "openAlertsBySeverity" contains "low"
    And the field "openAlertsBySeverity" contains "medium"
    And the field "openAlertsBySeverity" contains "high"
    And the field "openAlertsBySeverity" contains "critical"
    And the field "openAlertsBySeverity" contains "total"
