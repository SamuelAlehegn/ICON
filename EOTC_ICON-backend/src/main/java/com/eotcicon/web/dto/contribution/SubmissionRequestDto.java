package com.eotcicon.web.dto.contribution;

import jakarta.validation.constraints.Size;

public record SubmissionRequestDto(
		@Size(max = 4000, message = "Notes must be at most 4000 characters")
		String notes) {
}
