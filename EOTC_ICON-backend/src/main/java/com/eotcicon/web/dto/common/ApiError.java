package com.eotcicon.web.dto.common;

import java.time.Instant;
import java.util.List;

/**
 * Consistent error body returned by the global exception handler.
 */
public record ApiError(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		List<FieldErrorDto> fieldErrors) {

	public ApiError {
		timestamp = timestamp == null ? Instant.now() : timestamp;
		fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
	}

	public static ApiError of(int status, String error, String message, String path) {
		return new ApiError(Instant.now(), status, error, message, path, List.of());
	}

	public static ApiError validation(int status, String error, String message, String path,
			List<FieldErrorDto> fieldErrors) {
		return new ApiError(Instant.now(), status, error, message, path, fieldErrors);
	}

}
