package com.eotcicon.web.dto.contribution;

import com.eotcicon.domain.enums.PublicationStatus;

/**
 * Receipt for an accepted contribution.
 */
public record ContributionCreatedDto(
		String referenceCode,
		PublicationStatus status,
		String message) {
}
