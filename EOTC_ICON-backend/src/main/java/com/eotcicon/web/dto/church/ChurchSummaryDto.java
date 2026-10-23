package com.eotcicon.web.dto.church;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Location and identity of a church as embedded in icon and saint responses.
 */
public record ChurchSummaryDto(
		UUID id,
		String slug,
		String name,
		String region,
		String town,
		String country,
		BigDecimal latitude,
		BigDecimal longitude) {
}
