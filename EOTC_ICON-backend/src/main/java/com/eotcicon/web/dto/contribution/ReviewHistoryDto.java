package com.eotcicon.web.dto.contribution;

import java.time.Instant;
import java.util.UUID;

import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.domain.enums.ReviewAction;

/**
 * Immutable moderation event shown to moderators and to the contributor who
 * owns the submission.
 */
public record ReviewHistoryDto(
		UUID id,
		ReviewAction action,
		PublicationStatus fromStatus,
		PublicationStatus toStatus,
		String notes,
		UUID reviewerId,
		String reviewerDisplayName,
		Instant createdAt) {
}
