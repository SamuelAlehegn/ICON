package com.eotcicon.web.dto.contribution;

import java.time.Instant;

import com.eotcicon.domain.enums.PublicationStatus;

/**
 * Anonymous submission status lookup by reference code.
 */
public record ContributionStatusDto(
		String referenceCode,
		PublicationStatus status,
		String title,
		String churchName,
		String depictedFigure,
		int imageCount,
		Instant submittedAt,
		Instant updatedAt,
		String moderatorNotes) {
}
