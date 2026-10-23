package com.eotcicon.web.dto.icon;

import com.eotcicon.domain.enums.LicenseType;

/**
 * Public rights and attribution statement of a single image. It never contains
 * verifier identity; that is an internal moderation concern.
 */
public record ImageRightsDto(
		String rightsHolder,
		LicenseType licenseType,
		String licenseUrl,
		String licenseNotes,
		String attributionText,
		Integer copyrightYear,
		boolean verified) {
}
