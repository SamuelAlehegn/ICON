package com.eotcicon.config;

import java.util.List;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile("prod")
public class ProdDatabaseConfig {

	private static final List<String> REQUIRED = List.of("DB_URL", "DB_USERNAME", "DB_PASSWORD");

	@Bean
	static BeanFactoryPostProcessor databaseEnvironmentValidator(Environment environment) {
		return beanFactory -> {
			for (String key : REQUIRED) {
				String value = environment.getProperty(key);
				if (value == null || value.isBlank()) {
					throw new IllegalStateException(
							"Missing required environment variable '" + key + "'. The 'prod' profile needs "
									+ "DB_URL, DB_USERNAME and DB_PASSWORD for PostgreSQL. For local development "
									+ "run without SPRING_PROFILES_ACTIVE (defaults to 'local', in-memory H2).");
				}
			}
		};
	}

}
