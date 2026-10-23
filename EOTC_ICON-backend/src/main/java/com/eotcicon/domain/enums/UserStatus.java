package com.eotcicon.domain.enums;

/**
 * Lifecycle status of a registered account. The values must stay in sync with
 * the {@code ck_users_status} check constraint of the schema.
 */
public enum UserStatus {
	ACTIVE,
	SUSPENDED,
	PENDING_VERIFICATION

}
