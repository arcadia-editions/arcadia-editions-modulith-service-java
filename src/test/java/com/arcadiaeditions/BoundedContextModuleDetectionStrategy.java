package com.arcadiaeditions;

import java.util.Set;
import java.util.stream.Stream;

import org.springframework.modulith.core.ApplicationModuleDetectionStrategy;
import org.springframework.modulith.core.JavaPackage;

/**
 * Detects one module per bounded context ({@code <area>.<context>}, e.g. {@code catalog.inventory} and
 * {@code catalog.products}), plus the shared top level packages {@code common} and {@code config}. The default
 * direct-sub-packages strategy would fold both catalog contexts into a single module {@code catalog}.
 *
 * <p>
 * Registered for tests only, in {@code src/test/resources/META-INF/spring.factories}.
 * </p>
 */
public class BoundedContextModuleDetectionStrategy implements ApplicationModuleDetectionStrategy {

    static final Set<String> SHARED_PACKAGES = Set.of("common", "config");

    @Override
    public Stream<JavaPackage> getModuleBasePackages(JavaPackage basePackage) {
        return basePackage.getDirectSubPackages().stream()
                .flatMap(area -> SHARED_PACKAGES.contains(area.getLocalName()) ? Stream.of(area)
                        : area.getDirectSubPackages().stream());
    }
}
