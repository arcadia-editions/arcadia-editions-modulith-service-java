package com.arcadiaeditions;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Verifies the Spring Modulith module structure (no cycles, no access to non-exposed packages).
 */
class ModularityTest {

    @Test
    void verifiesModularStructure() {
        ApplicationModules.of(Application.class).verify();
    }
}
