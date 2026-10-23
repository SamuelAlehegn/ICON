package com.eotcicon.web.dto.church;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.eotcicon.domain.enums.RecordStatus;
import com.eotcicon.web.dto.icon.IconSummaryDto;

/**
 * Full public representation of a church and the published icons it holds.
 */
public record ChurchDetailDto(
		UUID id,
		String slug,
		String name,
		String region,
		String town,
		String country,
		BigDecimal latitude,
		BigDecimal longitude,
		String description,
		RecordStatus status,
		List<String> aliases,
		long iconCount,
		List<IconSummaryDto> icons,
		Instant createdAt,
		Instant updatedAt) {

	public ChurchDetailDto {
		aliases = aliases == null ? List.of() : List.copyOf(aliases);
		icons = icons == null ? List.of() : List.copyOf(icons);
	}

}
