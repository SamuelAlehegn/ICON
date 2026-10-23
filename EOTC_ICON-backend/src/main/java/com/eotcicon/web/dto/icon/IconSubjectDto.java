package com.eotcicon.web.dto.icon;

import java.util.UUID;

import com.eotcicon.domain.enums.FigureCertainty;
import com.eotcicon.web.dto.saint.SaintSummaryDto;

/**
 * A figure depicted on an icon. {@code saint} is null when the figure has not
 * been identified; {@code subjectLabel} remains the caption on the artwork.
 */
public record IconSubjectDto(
		UUID id,
		String subjectLabel,
		FigureCertainty figureCertainty,
		boolean primary,
		int sortOrder,
		SaintSummaryDto saint) {
}
