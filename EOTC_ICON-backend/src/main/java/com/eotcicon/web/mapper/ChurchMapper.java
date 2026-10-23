package com.eotcicon.web.mapper;

import java.util.List;
import java.util.Objects;

import com.eotcicon.domain.Church;
import com.eotcicon.domain.ChurchAlias;
import com.eotcicon.web.dto.church.ChurchDetailDto;
import com.eotcicon.web.dto.church.ChurchMarkerDto;
import com.eotcicon.web.dto.church.ChurchSummaryDto;
import com.eotcicon.web.dto.icon.IconSummaryDto;

/**
 * Manual mapping from {@link Church} to its public DTOs.
 */
public final class ChurchMapper {

	private ChurchMapper() {
	}

	public static ChurchSummaryDto toSummary(Church church) {
		if (church == null) {
			return null;
		}
		return new ChurchSummaryDto(
				church.getId(),
				church.getSlug(),
				church.getName(),
				church.getRegion(),
				church.getTown(),
				church.getCountry(),
				church.getLatitude(),
				church.getLongitude());
	}

	public static ChurchDetailDto toDetail(Church church, List<IconSummaryDto> icons) {
		if (church == null) {
			return null;
		}
		List<IconSummaryDto> safeIcons = icons == null ? List.of() : List.copyOf(icons);
		return new ChurchDetailDto(
				church.getId(),
				church.getSlug(),
				church.getName(),
				church.getRegion(),
				church.getTown(),
				church.getCountry(),
				church.getLatitude(),
				church.getLongitude(),
				church.getDescription(),
				church.getStatus(),
				aliasNames(church),
				safeIcons.size(),
				safeIcons,
				church.getCreatedAt(),
				church.getUpdatedAt());
	}

	public static ChurchMarkerDto toMarker(Church church, long iconCount) {
		if (church == null) {
			return null;
		}
		return new ChurchMarkerDto(
				church.getId(),
				church.getSlug(),
				church.getName(),
				church.getRegion(),
				church.getTown(),
				church.getLatitude(),
				church.getLongitude(),
				iconCount);
	}

	public static List<String> aliasNames(Church church) {
		if (church == null || church.getAliases() == null) {
			return List.of();
		}
		return church.getAliases().stream()
				.map(ChurchAlias::getName)
				.filter(Objects::nonNull)
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.toList();
	}

}
