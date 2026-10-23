package com.eotcicon.web.dto.icon;

import java.util.List;
import java.util.UUID;

import com.eotcicon.domain.enums.FigureCertainty;

/**
 * Gallery-card representation of a published icon.
 */
public record IconSummaryDto(
		UUID id,
		String slug,
		String title,
		String depictedFigure,
		FigureCertainty figureCertainty,
		List<String> saintNames,
		String churchName,
		String churchSlug,
		String town,
		String region,
		String country,
		String contributorCredit,
		String thumbnailUrl,
		String imageUrl,
		String altText) {

	public IconSummaryDto {
		saintNames = saintNames == null ? List.of() : List.copyOf(saintNames);
	}

}
