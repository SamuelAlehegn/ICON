package com.eotcicon.web.mapper;

import com.eotcicon.web.dto.stats.ArchiveStatsDto;

/**
 * Maps the aggregate counts produced by the statistics query to the public DTO.
 */
public final class ArchiveStatsMapper {

	private ArchiveStatsMapper() {
	}

	public static ArchiveStatsDto toDto(
			long publishedIcons,
			long churches,
			long saints,
			long regions,
			long contributors,
			boolean sampleData) {
		return new ArchiveStatsDto(publishedIcons, churches, saints, regions, contributors, sampleData);
	}

}
