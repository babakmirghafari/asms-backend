package com.asms.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit tests enforcing the delegation pattern layer rules.
 *
 * <p>Critical rules:
 * <ul>
 *   <li>NO @RestController or @RequestMapping in THIS project — those belong to the contract JAR</li>
 *   <li>Services implement *ApiDelegate interfaces — no HTTP annotations</li>
 *   <li>Domain entities are in the domain package</li>
 * </ul>
 *
 * <p>Note: Only the backend implementation packages are checked here:
 * {@code com.asms.service}, {@code com.asms.config}, {@code com.asms.exception}.
 * The contract JAR's {@code com.asms.api} and {@code com.asms.model} are intentionally
 * excluded — they are generated and are allowed to have HTTP annotations.
 */
@DisplayName("Architecture Rules")
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        // Import only the backend implementation packages — explicitly exclude the contract packages
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
    }

    @Test
    @DisplayName("No @RestController annotations in backend implementation code")
    void noRestControllerAnnotations() {
        ArchRule rule = noClasses()
            .should()
            .beAnnotatedWith(RestController.class)
            .because("Controllers are generated and shipped in the contract artifact JAR — "
                + "this project must not contain any @RestController");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("No class-level @RequestMapping in backend implementation code")
    void noClassLevelRequestMapping() {
        ArchRule rule = noClasses()
            .should()
            .beAnnotatedWith(RequestMapping.class)
            .because("HTTP mapping belongs to the generated controllers in the contract artifact");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Service classes are in the service package")
    void serviceClassesAreInServicePackage() {
        ArchRule rule = classes()
            .that().areAnnotatedWith(Service.class)
            .should().resideInAPackage("com.asms.service..")
            .allowEmptyShould(true)
            .because("All @Service beans must be in com.asms.service");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Repository classes are in the repository package (when they exist)")
    void repositoryClassesAreInRepositoryPackage() {
        ArchRule rule = classes()
            .that().areAnnotatedWith(Repository.class)
            .should().resideInAPackage("com.asms.repository..")
            .allowEmptyShould(true)
            .because("All @Repository beans must be in com.asms.repository");

        rule.check(importedClasses);
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
            .because("Service delegates must contain only business logic — no HTTP concerns");

        rule.check(importedClasses);
    }
}
