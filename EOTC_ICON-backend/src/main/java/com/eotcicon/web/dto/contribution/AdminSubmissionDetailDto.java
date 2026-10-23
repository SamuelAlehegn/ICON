package com.eotcicon.web.dto.contribution;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.eotcicon.domain.enums.AttributionPreference;
import com.eotcicon.domain.enums.FigureCertainty;
import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.web.dto.church.ChurchSummaryDto;
import com.eotcicon.web.dto.icon.IconImageDto;
import com.eotcicon.web.dto.icon.IconSubjectDto;

/**
 * Moderator-only submission detail.
 */
public record AdminSubmissionDetailDto(
		UUID id,
		String referenceCode,
		String slug,
		String title,
		PublicationStatus status,
		String description,
		String historicalNotes,
		String category,
		FigureCertainty figureCertainty,
		LocalDate captureDate,
		LocalDate artworkDateStart,
		LocalDate artworkDateEnd,
		String contributorDisplayName,
		AttributionPreference attributionPreference,
		String contributorCredit,
		String contactEmail,
		Instant publishedAt,
		ChurchSummaryDto church,
		List<IconSubjectDto> subjects,
		List<IconImageDto> images,
		List<ReviewHistoryDto> reviews,
		Instant submittedAt,
		Instant updatedAt) {

	public AdminSubmissionDetailDto {
		subjects = subjects == null ? List.of() : List.copyOf(subjects);
		images = images == null ? List.of() : List.copyOf(images);
		reviews = reviews == null ? List.of() : List.copyOf(reviews);
	}

}
