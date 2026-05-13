package com.asms.bdd;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Cucumber BDD test suite entry point.
 *
 * <p>Discovers all feature files under {@code src/test/resources/features/}
 * and runs them with the step definitions in {@code com.asms.bdd.steps}.
 * The glue also includes {@code com.asms.bdd} for {@link CucumberSpringConfiguration}.
 *
 * <p>Scenarios tagged {@code @Pending} are skipped — they require domain entity and
 * repository wiring before they can pass. Remove the {@code @Pending} tag once
 * the relevant service implementation is complete.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.asms.bdd,com.asms.bdd.steps")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @Pending")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:target/cucumber-reports/cucumber.html, json:target/cucumber-reports/cucumber.json"
)
public class CucumberTestSuite {
    // Entry point — no additional configuration needed.
}
