package com.eotcicon.web.dto.church;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Minimal payload for a church pin on the public map.
 */
public record ChurchMarkerDto(
		UUID id,
		String slug,
		String name,
		String region,
		String town,
		BigDecimal latitude,
		BigDecimal longitude,
		long iconCount) {
}
