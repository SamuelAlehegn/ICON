package com.eotcicon.web.dto.contribution;

import java.time.LocalDate;

import com.eotcicon.domain.enums.AttributionPreference;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateSubmissionRequest(
		@Size(max = 240, message = "Title must be at most 240 characters")
		String title,

		@Size(max = 4000, message = "Description must be at most 4000 characters")
		String description,

		@Size(max = 4000, message = "Historical notes must be at most 4000 characters")
		String historicalNotes,

		@Size(max = 80, message = "Category must be at most 80 characters")
		String category,

		LocalDate captureDate,

		LocalDate artworkDateStart,

		LocalDate artworkDateEnd,

		AttributionPreference attributionPreference,

		@Size(max = 120, message = "Display name must be at most 120 characters")
		String contributorDisplayName,

		@Email(message = "Contact email must be a valid email address")
		@Size(max = 320, message = "Contact email must be at most 320 characters")
		String contactEmail) {
}
