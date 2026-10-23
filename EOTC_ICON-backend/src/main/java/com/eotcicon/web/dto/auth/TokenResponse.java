package com.eotcicon.web.dto.auth;

/**
 * Access and refresh tokens issued by the authentication endpoints.
 */
public record TokenResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long expiresIn) {
}
