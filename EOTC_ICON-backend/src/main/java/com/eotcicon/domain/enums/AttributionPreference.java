package com.eotcicon.domain.enums;

/**
 * Whether the contributor wants to be named publicly or kept anonymous. The
 * values must stay in sync with the {@code ck_icon_attribution_preference}
 * check constraint of the schema.
 */
public enum AttributionPreference {
	DISPLAY_NAME,
	ANONYMOUS

}
