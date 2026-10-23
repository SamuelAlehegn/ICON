package com.eotcicon.domain.enums;

/**
 * Editorial state of a church or saint record. The values must stay in sync
 * with the {@code ck_church_status} and {@code ck_saint_status} check
 * constraints of the schema.
 */
public enum RecordStatus {
	ACTIVE,
	MERGED,
	ARCHIVED

}
