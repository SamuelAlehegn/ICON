package com.eotcicon.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.eotcicon.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Value("${app.cors.allowed-origins:http://localhost:4200}")
	private String allowedOrigins;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			// API authentication is via Authorization bearer tokens, never cookies.
			.csrf(csrf -> csrf.disable())
			.headers(headers -> headers
				.frameOptions(frame -> frame.sameOrigin())
			)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers(
					"/",
					"/error",
					"/h2-console/**",
					"/swagger-ui/**",
					"/swagger-ui.html",
					"/v3/api-docs/**",
					"/v3/api-docs.yaml",
					"/api/v1/icons/**",
					"/api/v1/churches/**",
					"/api/v1/saints/**",
					"/api/v1/statistics/**",
					"/api/v1/contributions/**",
					"/api/v1/auth/register",
					"/api/v1/auth/login",
					"/api/v1/auth/refresh",
					"/media/**"
				).permitAll()
				.requestMatchers("/api/v1/admin/submissions", "/api/v1/admin/submissions/**")
					.hasAnyRole("MODERATOR", "ADMIN")
				.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
				.requestMatchers("/api/v1/users/**").hasAnyRole("CONTRIBUTOR", "MODERATOR", "ADMIN")
				.requestMatchers("/api/v1/mod/**").hasAnyRole("MODERATOR", "ADMIN")
				.anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		List<String> origins = Arrays.stream(allowedOrigins.split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.toList();
		configuration.setAllowedOrigins(origins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
		return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
	}

}
