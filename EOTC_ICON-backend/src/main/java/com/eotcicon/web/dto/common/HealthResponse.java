package com.eotcicon.web.dto.common;

import java.time.Instant;

/**
 * Liveness payload returned by health probes.
 */
public record HealthResponse(String status, String service, Instant timestamp) {

	public static HealthResponse up(String service) {
		return new HealthResponse("UP", service, Instant.now());
	}

}
