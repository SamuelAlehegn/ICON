package com.eotcicon.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration payload for creating a new contributor account.
 */
public record RegisterRequest(
		@NotBlank(message = "Email is required")
		@Email(message = "Email must be valid")
		@Size(max = 320, message = "Email must be at most 320 characters")
		String email,

		@NotBlank(message = "Password is required")
		@Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
		String password,

		@NotBlank(message = "Display name is required")
		@Size(max = 120, message = "Display name must be at most 120 characters")
		String displayName) {
}
