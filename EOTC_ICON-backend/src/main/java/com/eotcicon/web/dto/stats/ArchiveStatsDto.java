package com.eotcicon.web.dto.stats;

/**
 * Aggregate counts for the public archive landing page.
 */
public record ArchiveStatsDto(
		long publishedIcons,
		long churches,
		long saints,
		long regions,
		long contributors,
		boolean sampleData) {
}
