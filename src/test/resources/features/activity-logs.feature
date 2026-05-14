Feature: Activity Logs (User-Facing Activity Feed)
  As an ASMS user
  I want to see my recent activity history
  So that I can verify what actions have been performed on my account

  Background:
    Given the service is running

  # AC-11: listActivityLogs with type=ACTIVITY filter
  @Pending
  Scenario: Activity logs endpoint returns paginated activity feed
    When I request the activity logs for my organization
    Then the response status is 200
    And the response contains field "content"

  # AC-11: category filter
  @Pending
  Scenario: Activity logs can be filtered by category
    When I request activity logs with category "AUTHENTICATION"
    Then the response status is 200
    And the response contains field "content"

  # AC-11: date range filter
  @Pending
  Scenario: Activity logs can be filtered by date range
    When I request activity logs from "2026-01-01T00:00:00Z" to "2026-12-31T23:59:59Z"
    Then the response status is 200
    And the response contains field "content"
