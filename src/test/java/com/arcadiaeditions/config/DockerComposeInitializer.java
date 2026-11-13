package com.arcadiaeditions.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Starts one PostgreSQL per JVM (Testcontainers, random host port) and points DATASOURCE_URL at it.
 * Self-contained: needs only Docker, no compose and no fixed ports, so clones can run at once.
 * Name kept from the generated initializer so existing @EnableDockerCompose usages keep working.
 */
public class DockerComposeInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(DockerComposeInitializer.class);

    /**
     * Use this annotation to activate TestContainers in your test.
     */
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @org.springframework.test.context.ContextConfiguration(initializers = DockerComposeInitializer.class)
    public @interface EnableDockerCompose {}

    private static GenericContainer<?> postgres;

    private static synchronized GenericContainer<?> postgres() {
        if (postgres == null) {
            postgres = new GenericContainer<>("postgres:16.3")
                    .withEnv("POSTGRES_DB", "app")
                    .withEnv("POSTGRES_USER", "postgres")
                    .withEnv("POSTGRES_PASSWORD", "admin123")
                    .withExposedPorts(5432)
                    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2));
            postgres.start();
            log.info("PostgreSQL started on {}:{}", postgres.getHost(), postgres.getMappedPort(5432));
        }
        return postgres;
    }

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        GenericContainer<?> pg = postgres();
        TestPropertyValues.of("DATASOURCE_URL=jdbc:postgresql://%s:%d/app".formatted(pg.getHost(), pg.getMappedPort(5432)))
                .applyTo(ctx.getEnvironment());
    }
}
