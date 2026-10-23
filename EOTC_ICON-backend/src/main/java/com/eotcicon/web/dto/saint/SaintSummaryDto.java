package com.eotcicon.web.dto.saint;

import java.util.UUID;

/**
 * Directory and embedded representation of a saint. {@code iconCount} is null
 * when the summary is embedded in an icon response and no count was requested.
 */
public record SaintSummaryDto(
		UUID id,
		String slug,
		String canonicalName,
		String amharicName,
		String geezName,
		String category,
		boolean verified,
		Long iconCount) {
}
