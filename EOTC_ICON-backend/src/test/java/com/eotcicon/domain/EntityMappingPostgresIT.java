package com.eotcicon.domain;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Runs the entity mapping contract against a real PostgreSQL instance, so the
 * production engine proves the same mapping as the local H2 mirror.
 *
 * <p>
 * The test is skipped unless {@code EOTC_PG_IT_URL} points at a PostgreSQL
 * database that the test user may create tables in.
 */
@ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "EOTC_PG_IT_URL", matches = ".+")
class EntityMappingPostgresIT extends AbstractEntityMappingTest {

	@DynamicPropertySource
	static void postgresProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> System.getenv("EOTC_PG_IT_URL"));
		registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("EOTC_PG_IT_USER", "postgres"));
		registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("EOTC_PG_IT_PASSWORD", "postgres"));
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("spring.flyway.locations", () -> "classpath:db/migration");
	}

}
