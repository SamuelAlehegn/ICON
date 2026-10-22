package com.eotcicon.config;

import java.util.List;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/**
 * Guards the PostgreSQL-backed profiles ({@code docker} and {@code prod}).
 *
 * <p>Both profiles read the connection details from the environment and have
 * no defaults, so a missing variable must fail fast with a readable message
 * instead of a placeholder-resolution error deep inside the datasource.
 */
@Configuration
@Profile({ "docker", "prod" })
public class PostgresDatabaseConfig {

	private static final List<String> REQUIRED = List.of("DB_URL", "DB_USERNAME", "DB_PASSWORD");

	@Bean
	static BeanFactoryPostProcessor databaseEnvironmentValidator(Environment environment) {
		return beanFactory -> {
			for (String key : REQUIRED) {
				String value = environment.getProperty(key);
				if (value == null || value.isBlank()) {
					throw new IllegalStateException(
							"Missing required environment variable '" + key + "'. The 'docker' and 'prod' "
								+ "profiles need DB_URL, DB_USERNAME and DB_PASSWORD for PostgreSQL. For local "
								+ "development run without SPRING_PROFILES_ACTIVE (defaults to 'local', H2 — no "
								+ "Docker required), or use 'docker compose up'.");
				}
			}
		};
	}

}
