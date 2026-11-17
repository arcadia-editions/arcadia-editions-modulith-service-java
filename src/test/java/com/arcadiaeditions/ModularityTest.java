package com.arcadiaeditions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.VerificationOptions;
import org.springframework.modulith.core.Violation;
import org.springframework.modulith.core.Violations;

/**
 * Verifies the Spring Modulith module boundaries: no module may use a type another module does not expose (a
 * non-exposed type or a named interface it does not declare), nor depend on a module its allowedDependencies leave out.
 *
 * <p>
 * Two checks of the plain {@code verify()} are left out on purpose (decision of the person, 2026-11-13):
 * </p>
 * <ul>
 * <li>cycle detection: the cycles between modules come from the generated event listener topology of the ZFL
 * choreography (each listener takes the producer's event type), and that topology will not change upstream;</li>
 * <li>the jMolecules verifications (layering): left out for the same reason as in {@link JMoleculesArchitectureTest},
 * a three tier web -> service -> repository where repositories name their aggregate.</li>
 * </ul>
 */
class ModularityTest {

    static final String CYCLE_REPORT_PREFIX = "Cycle detected:";

    static boolean isCycleReport(Violation violation) {
        return violation.getMessage().startsWith(CYCLE_REPORT_PREFIX);
    }

    static final Set<String> EXPECTED_MODULES = Set.of("catalog.inventory", "catalog.products", "fulfillment.shipping",
            "notifications.consumer", "orders.checkout", "payments.processing", "common", "config");

    @Test
    void detectsTheBoundedContextsAsSeparateModules() {
        var modules = ApplicationModules.of(Application.class);

        assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
                .containsExactlyInAnyOrderElementsOf(EXPECTED_MODULES);
    }

    @Test
    void verifiesModuleBoundariesWithoutCyclesNorJMoleculesLayering() {
        var modules = ApplicationModules.of(Application.class);

        Violations violations = modules
                .detectViolations(VerificationOptions.defaults().withoutAdditionalVerifications())
                .filter(violation -> !isCycleReport(violation));

        assertThat(violations.getMessages()).isEmpty();
    }
}
