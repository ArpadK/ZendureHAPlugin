package nl.jpoint.zendure;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture tests using ArchUnit to enforce layered architecture constraints.
 *
 * <p>Ensures that production code conforms to layered architecture:
 * - automation package does not depend on homeassistant or zendure infrastructure
 * - domain package does not depend on infrastructure (homeassistant, zendure)
 *
 * <p>Note: Test classes may depend on infrastructure for testing purposes (e.g., stubs).
 * These tests import only production code packages to enforce the constraints.
 */
public class ArchitectureTest {

    private static final JavaClasses productionClasses = new ClassFileImporter()
        .importPackages(
            "nl.jpoint.zendure.automation",
            "nl.jpoint.zendure.domain",
            "nl.jpoint.zendure.config",
            "nl.jpoint.zendure.homeassistant",
            "nl.jpoint.zendure.zendure"
        );

    @Test
    void automationMustNotDependOnHomeAssistantOrZendure() {
        ArchRule automationIndependence = noClasses()
            .that().resideInAPackage("nl.jpoint.zendure.automation..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "nl.jpoint.zendure.homeassistant..",
                "nl.jpoint.zendure.zendure.."
            );

        automationIndependence.check(productionClasses);
    }

    @Test
    void domainMustNotDependOnInfrastructure() {
        ArchRule domainIndependence = noClasses()
            .that().resideInAPackage("nl.jpoint.zendure.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "nl.jpoint.zendure.homeassistant..",
                "nl.jpoint.zendure.zendure.."
            );

        domainIndependence.check(productionClasses);
    }
}
