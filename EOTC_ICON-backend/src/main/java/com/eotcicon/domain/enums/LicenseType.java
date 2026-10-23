package com.eotcicon.domain.enums;

/**
 * Licence of an uploaded image. The values must stay in sync with the
 * {@code ck_image_rights_license_type} check constraint of the schema.
 */
public enum LicenseType {
	PUBLIC_DOMAIN,
	CC_BY,
	CC_BY_SA,
	CC_BY_NC,
	CC_BY_NC_ND,
	ALL_RIGHTS_RESERVED,
	PERMISSION_GRANTED,
	UNKNOWN

}
