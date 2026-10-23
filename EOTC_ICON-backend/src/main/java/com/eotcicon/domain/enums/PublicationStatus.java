package com.eotcicon.domain.enums;

/**
 * Moderation state of an icon contribution. The values must stay in sync with
 * the {@code ck_icon_publication_status} and
 * {@code ck_contribution_review_status} check constraints of the schema.
 */
public enum PublicationStatus {
	PENDING,
	APPROVED,
	REJECTED,
	NEEDS_CORRECTION,
	ARCHIVED

}
