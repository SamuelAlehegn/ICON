package com.eotcicon.web.dto.saint;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.eotcicon.domain.enums.RecordStatus;
import com.eotcicon.web.dto.church.ChurchSummaryDto;
import com.eotcicon.web.dto.icon.IconSummaryDto;

/**
 * Full public representation of a saint, including the published depictions
 * and the churches that hold them.
 */
public record SaintDetailDto(
		UUID id,
		String slug,
		String canonicalName,
		String amharicName,
		String geezName,
		String category,
		String description,
		boolean verified,
		RecordStatus status,
		List<String> aliases,
		long iconCount,
		List<IconSummaryDto> depictions,
		List<ChurchSummaryDto> churches,
		Instant createdAt,
		Instant updatedAt) {

	public SaintDetailDto {
		aliases = aliases == null ? List.of() : List.copyOf(aliases);
		depictions = depictions == null ? List.of() : List.copyOf(depictions);
		churches = churches == null ? List.of() : List.copyOf(churches);
	}

}
