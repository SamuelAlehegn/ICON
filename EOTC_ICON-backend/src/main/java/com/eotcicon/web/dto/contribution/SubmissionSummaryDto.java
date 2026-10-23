package com.eotcicon.web.dto.contribution;

import java.time.Instant;
import java.util.UUID;

import com.eotcicon.domain.enums.PublicationStatus;

/**
 * Compact row used by the moderator queue and a contributor's own submission
 * list.
 */
public record SubmissionSummaryDto(
		UUID id,
		String referenceCode,
		String title,
		String depictedFigure,
		String churchName,
		PublicationStatus status,
		int imageCount,
		Instant submittedAt,
		Instant updatedAt) {
}
