package com.eotcicon.web.dto.contribution;

import com.eotcicon.domain.enums.ReviewAction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Moderator decision applied to a pending submission.
 */
public record ReviewDecisionRequest(
		@NotNull(message = "Action is required")
		ReviewAction action,

		@Size(max = 4000, message = "Notes must be at most 4000 characters")
		String notes) {
}
