package com.eotcicon.web.dto.auth;

import java.time.Instant;
import java.util.UUID;

import com.eotcicon.domain.enums.UserRole;
import com.eotcicon.domain.enums.UserStatus;

/**
 * Public profile of the authenticated caller. Password hashes and tokens are
 * intentionally absent.
 */
public record UserProfileDto(
		UUID id,
		String email,
		String displayName,
		UserRole role,
		UserStatus status,
		Instant lastLoginAt,
		Instant createdAt) {
}
