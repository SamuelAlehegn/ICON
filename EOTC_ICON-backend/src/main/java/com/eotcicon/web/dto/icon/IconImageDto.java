package com.eotcicon.web.dto.icon;

import java.time.Instant;
import java.util.UUID;

/**
 * A photograph of an icon. Storage keys stay internal; callers receive resolved
 * public URLs instead.
 */
public record IconImageDto(
		UUID id,
		String imageUrl,
		String thumbnailUrl,
		String altText,
		String contentType,
		long fileSizeBytes,
		Integer widthPx,
		Integer heightPx,
		boolean primary,
		int sortOrder,
		Instant capturedAt,
		ImageRightsDto rights) {
}
