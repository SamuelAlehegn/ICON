package com.eotcicon.web.dto.contribution;

import java.time.Instant;
import java.util.UUID;

import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.domain.enums.ReviewAction;

/**
 * Result of applying a moderation decision, including the submission's new
 * state.
 */
public record ReviewDecisionResponse(
		UUID submissionId,
		ReviewAction action,
		PublicationStatus status,
		String message,
		Instant timestamp) {

	public static ReviewDecisionResponse of(UUID submissionId, ReviewAction action, PublicationStatus status,
			String message) {
		return new ReviewDecisionResponse(submissionId, action, status, message, Instant.now());
	}

}
