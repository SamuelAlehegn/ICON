package com.eotcicon.web.dto.icon;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.eotcicon.domain.enums.FigureCertainty;
import com.eotcicon.web.dto.church.ChurchSummaryDto;

/**
 * Full public representation of a published icon.
 */
public record IconDetailDto(
		UUID id,
		String slug,
		String referenceCode,
		String title,
		String description,
		String historicalNotes,
		String category,
		FigureCertainty figureCertainty,
		LocalDate captureDate,
		LocalDate artworkDateStart,
		LocalDate artworkDateEnd,
		String contributorCredit,
		Instant publishedAt,
		ChurchSummaryDto church,
		List<IconSubjectDto> subjects,
		List<IconImageDto> images,
		List<IconSummaryDto> relatedIcons,
		List<IconSummaryDto> moreFromChurch,
		Instant createdAt,
		Instant updatedAt) {

	public IconDetailDto {
		subjects = subjects == null ? List.of() : List.copyOf(subjects);
		images = images == null ? List.of() : List.copyOf(images);
		relatedIcons = relatedIcons == null ? List.of() : List.copyOf(relatedIcons);
		moreFromChurch = moreFromChurch == null ? List.of() : List.copyOf(moreFromChurch);
	}

}
