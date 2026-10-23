package com.eotcicon.web.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * A refresh token supplied in the request body for rotation or revocation.
 */
public record RefreshTokenRequest(
		@NotBlank(message = "Refresh token is required")
		String refreshToken) {
}
