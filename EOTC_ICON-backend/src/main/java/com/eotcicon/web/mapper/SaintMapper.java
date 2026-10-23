package com.eotcicon.web.mapper;

import java.util.List;
import java.util.Objects;

import com.eotcicon.domain.Saint;
import com.eotcicon.domain.SaintAlias;
import com.eotcicon.web.dto.church.ChurchSummaryDto;
import com.eotcicon.web.dto.icon.IconSummaryDto;
import com.eotcicon.web.dto.saint.SaintDetailDto;
import com.eotcicon.web.dto.saint.SaintSummaryDto;

/**
 * Manual mapping from {@link Saint} to its public DTOs.
 */
public final class SaintMapper {

	private SaintMapper() {
	}

	public static SaintSummaryDto toSummary(Saint saint) {
		return toSummary(saint, null);
	}

	public static SaintSummaryDto toSummary(Saint saint, Long iconCount) {
		if (saint == null) {
			return null;
		}
		return new SaintSummaryDto(
				saint.getId(),
				saint.getSlug(),
				saint.getCanonicalName(),
				saint.getAmharicName(),
				saint.getGeezName(),
				saint.getCategory(),
				saint.isVerified(),
				iconCount);
	}

	public static SaintDetailDto toDetail(Saint saint, List<IconSummaryDto> depictions,
			List<ChurchSummaryDto> churches) {
		if (saint == null) {
			return null;
		}
		List<IconSummaryDto> safeDepictions = depictions == null ? List.of() : List.copyOf(depictions);
		List<ChurchSummaryDto> safeChurches = churches == null ? List.of() : List.copyOf(churches);
		return new SaintDetailDto(
				saint.getId(),
				saint.getSlug(),
				saint.getCanonicalName(),
				saint.getAmharicName(),
				saint.getGeezName(),
				saint.getCategory(),
				saint.getDescription(),
				saint.isVerified(),
				saint.getStatus(),
				aliasNames(saint),
				safeDepictions.size(),
				safeDepictions,
				safeChurches,
				saint.getCreatedAt(),
				saint.getUpdatedAt());
	}

	public static List<String> aliasNames(Saint saint) {
		if (saint == null || saint.getAliases() == null) {
			return List.of();
		}
		return saint.getAliases().stream()
				.map(SaintAlias::getName)
				.filter(Objects::nonNull)
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.toList();
	}

}
