package com.eotcicon.domain.enums;

/**
 * Role of a registered account. The values must stay in sync with the
 * {@code ck_users_role} check constraint of the schema.
 */
public enum UserRole {
	CONTRIBUTOR,
	MODERATOR,
	ADMIN

}
