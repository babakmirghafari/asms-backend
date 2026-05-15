package com.asms.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit tests enforcing the Handler/Service split pattern layer rules.
 *
 * <p>Architecture:
 * <ul>
 *   <li>Handlers ({@code com.asms.handler}) implement {@code *ApiDelegate} — REST adapters only</li>
 *   <li>Services ({@code com.asms.service}) contain business logic — NO {@code *ApiDelegate} implementation</li>
 *   <li>NO {@code @RestController} or {@code @RequestMapping} in THIS project — those belong to the contract JAR</li>
 * </ul>
 *
 * <p>Note: The contract JAR's {@code com.asms.api} and {@code com.asms.model} are intentionally
 * excluded — they are generated and are allowed to have HTTP annotations.
 */
@DisplayName("Architecture Rules")
class ArchitectureTest {

    private static JavaClasses importedClasses;
    private static JavaClasses allBackendClasses;

    @BeforeAll
    static void importClasses() {
        // Service-layer classes only — for service-specific rules
        importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(
                "com.asms.service",
                "com.asms.config",
                "com.asms.exception",
                "com.asms.domain",
                "com.asms.repository",
                "com.asms.mapper"
            );

        // All backend classes including handlers — for cross-layer rules
        allBackendClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(
                "com.asms.service",
                "com.asms.handler",
                "com.asms.config",
                "com.asms.exception",
                "com.asms.domain",
                "com.asms.repository",
                "com.asms.mapper"
            );
    }

    // ─── Handler/Service Split Rules (post-refactor guards) ──────────────────

    @Test
    @DisplayName("No class in com.asms.service implements *ApiDelegate")
    void servicesMustNotImplementApiDelegate() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("com.asms.service..")
            .should().implement(com.tngtech.archunit.base.DescribedPredicate.describe(
                "any interface whose name ends with 'ApiDelegate'",
                javaClass -> javaClass.isInterface() && javaClass.getSimpleName().endsWith("ApiDelegate")
            ))
            .allowEmptyShould(true)
            .because("After the handler/service split, business logic services must NOT implement "
                + "*ApiDelegate. Only handlers in com.asms.handler may implement *ApiDelegate.");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Handler classes in com.asms.handler are annotated with @Component")
    void handlerClassesAreComponents() {
        ArchRule rule = classes()
            .that().resideInAPackage("com.asms.handler")
            .and().haveSimpleNameEndingWith("Handler")
            .should().beAnnotatedWith(Component.class)
            .allowEmptyShould(true)
            .because("All handler REST adapters must be Spring @Component beans "
                + "so they are discovered by the auto-scan");

        rule.check(allBackendClasses);
    }

    @Test
    @DisplayName("Handler classes (API adapters) only reside in com.asms.handler")
    void handlerClassesAreInHandlerPackage() {
        // GlobalExceptionHandler is in com.asms.exception and is intentionally excluded
        // This rule targets only API adapter handlers (those annotated with @Component)
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Handler")
            .and().areAnnotatedWith(Component.class)
            .should().resideInAPackage("com.asms.handler..")
            .allowEmptyShould(true)
            .because("All @Component handler REST adapters must be in com.asms.handler");

        rule.check(allBackendClasses);
    }

    // ─── Existing delegation pattern rules ───────────────────────────────────

    @Test
    @DisplayName("No @RestController annotations in backend implementation code")
    void noRestControllerAnnotations() {
        ArchRule rule = noClasses()
            .should()
            .beAnnotatedWith(RestController.class)
            .because("Controllers are generated and shipped in the contract artifact JAR — "
                + "this project must not contain any @RestController");

        rule.check(allBackendClasses);
    }

    @Test
    @DisplayName("No class-level @RequestMapping in backend implementation code")
    void noClassLevelRequestMapping() {
        ArchRule rule = noClasses()
            .should()
            .beAnnotatedWith(RequestMapping.class)
            .because("HTTP mapping belongs to the generated controllers in the contract artifact");

        rule.check(allBackendClasses);
    }

    @Test
    @DisplayName("Service classes are in the service package")
    void serviceClassesAreInServicePackage() {
        ArchRule rule = classes()
            .that().areAnnotatedWith(Service.class)
            .should().resideInAPackage("com.asms.service..")
            .allowEmptyShould(true)
            .because("All @Service beans must be in com.asms.service");

        rule.check(allBackendClasses);
    }

    @Test
    @DisplayName("Repository classes are in the repository package (when they exist)")
    void repositoryClassesAreInRepositoryPackage() {
        ArchRule rule = classes()
            .that().areAnnotatedWith(Repository.class)
            .should().resideInAPackage("com.asms.repository..")
            .allowEmptyShould(true)
            .because("All @Repository beans must be in com.asms.repository");

        rule.check(allBackendClasses);
    }

    @Test
    @DisplayName("Services do not directly depend on web layer")
    void servicesDontDependOnWebLayer() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("com.asms.service..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework.web.bind.annotation..",
                "jakarta.servlet.http.."
            )
            .allowEmptyShould(true)
            .because("Domain services must contain only business logic — no HTTP concerns. "
                + "HTTP adaption belongs to com.asms.handler classes.");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Handlers only depend on the service layer (not repositories directly)")
    void handlersDontDependOnRepositoriesDirectly() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("com.asms.handler..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.asms.repository..")
            .allowEmptyShould(true)
            .because("Handlers are REST adapters and must only call services, not repositories directly. "
                + "Repository access belongs to the service layer.");

        rule.check(allBackendClasses);
    }
}
