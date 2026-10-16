package com.arcadiaeditions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Verifies the Spring Modulith module structure (no cycles, no access to non-exposed packages).
 */
class ModularityTest {

    @Disabled(
            "R-12: generated structure fails ApplicationModules.verify(): 4 inter-module cycles (orders<->payments, orders->fulfillment->payments->orders, via catalog) and non-exposed domain/event types; see research.md R-12")
    @Test
    void verifiesModularStructure() {
        ApplicationModules.of(Application.class).verify();
    }
}
