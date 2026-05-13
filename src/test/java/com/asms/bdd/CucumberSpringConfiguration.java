package com.asms.bdd;

import com.asms.config.TestSecurityConfig;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Bridges Cucumber's step context with the Spring Boot test context.
 *
 * <p>Uses a static PostgreSQL container started before Spring context initialization,
 * ensuring the JDBC URL is available when @DynamicPropertySource is evaluated.
 *
 * <p>Imports TestSecurityConfig to permit all requests — BDD tests focus on
 * business behaviour rather than authentication mechanics.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class CucumberSpringConfiguration {

    @SuppressWarnings({"resource", "rawtypes"})
    static final PostgreSQLContainer POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("asms_bdd_test")
            .withUsername("asms_test")
            .withPassword("asms_test");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
