package com.eotcicon.web.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.data.domain.Page;

import com.eotcicon.domain.Church;
import com.eotcicon.domain.Icon;
import com.eotcicon.domain.IconImage;
import com.eotcicon.domain.IconSubject;
import com.eotcicon.domain.ImageRights;
import com.eotcicon.domain.Saint;
import com.eotcicon.domain.enums.AttributionPreference;
import com.eotcicon.web.dto.common.PageResponse;
import com.eotcicon.web.dto.icon.IconDetailDto;
import com.eotcicon.web.dto.icon.IconImageDto;
import com.eotcicon.web.dto.icon.IconSubjectDto;
import com.eotcicon.web.dto.icon.IconSummaryDto;
import com.eotcicon.web.dto.icon.ImageRightsDto;

/**
 * Manual mapping from the icon aggregate to its public DTOs.
 */
public final class IconMapper {

	private static final String ANONYMOUS_CREDIT = "Anonymous contributor";

	private static final Comparator<IconImage> IMAGE_ORDER = Comparator
			.comparing(IconImage::isPrimary)
			.reversed()
			.thenComparingInt(IconImage::getSortOrder);

	private static final Comparator<IconSubject> SUBJECT_ORDER = Comparator
			.comparing(IconSubject::isPrimary)
			.reversed()
			.thenComparingInt(IconSubject::getSortOrder);

	private IconMapper() {
	}

	public static IconSummaryDto toSummary(Icon icon, Function<String, String> mediaUrlResolver) {
		Objects.requireNonNull(icon, "icon");
		IconImage primaryImage = primaryImage(icon);
		Church church = icon.getChurch();
		String thumbnailKey = primaryImage == null ? null
				: firstNonBlank(primaryImage.getThumbnailStorageKey(), primaryImage.getStorageKey());

		return new IconSummaryDto(
				icon.getId(),
				icon.getSlug(),
				icon.getTitle(),
				depictedFigure(icon),
				icon.getFigureCertainty(),
				saintNames(icon),
				church == null ? null : church.getName(),
				church == null ? null : church.getSlug(),
				church == null ? null : church.getTown(),
				church == null ? null : church.getRegion(),
				church == null ? null : church.getCountry(),
				contributorCredit(icon),
				resolve(mediaUrlResolver, thumbnailKey),
				resolve(mediaUrlResolver, primaryImage == null ? null : primaryImage.getStorageKey()),
				primaryImage == null ? null : primaryImage.getAltText());
	}

	public static PageResponse<IconSummaryDto> toSummaryPage(Page<Icon> page,
			Function<String, String> mediaUrlResolver) {
		return PageResponse.of(page,
				page.getContent().stream().map(icon -> toSummary(icon, mediaUrlResolver)).toList());
	}

	public static IconDetailDto toDetail(Icon icon, Function<String, String> mediaUrlResolver) {
		return toDetail(icon, mediaUrlResolver, List.of(), List.of());
	}

	public static IconDetailDto toDetail(Icon icon, Function<String, String> mediaUrlResolver,
			List<IconSummaryDto> relatedIcons, List<IconSummaryDto> moreFromChurch) {
		Objects.requireNonNull(icon, "icon");
		return new IconDetailDto(
				icon.getId(),
				icon.getSlug(),
				icon.getReferenceCode(),
				icon.getTitle(),
				icon.getDescription(),
				icon.getHistoricalNotes(),
				icon.getCategory(),
				icon.getFigureCertainty(),
				icon.getCaptureDate(),
				icon.getArtworkDateStart(),
				icon.getArtworkDateEnd(),
				contributorCredit(icon),
				icon.getPublishedAt(),
				ChurchMapper.toSummary(icon.getChurch()),
				subjects(icon),
				images(icon, mediaUrlResolver),
				relatedIcons,
				moreFromChurch,
				icon.getCreatedAt(),
				icon.getUpdatedAt());
	}

	public static IconImageDto toImageDto(IconImage image, Function<String, String> mediaUrlResolver) {
		if (image == null) {
			return null;
		}
		String thumbnailKey = firstNonBlank(image.getThumbnailStorageKey(), image.getStorageKey());
		return new IconImageDto(
				image.getId(),
				resolve(mediaUrlResolver, image.getStorageKey()),
				resolve(mediaUrlResolver, thumbnailKey),
				image.getAltText(),
				image.getContentType(),
				image.getFileSizeBytes(),
				image.getWidthPx(),
				image.getHeightPx(),
				image.isPrimary(),
				image.getSortOrder(),
				image.getCapturedAt(),
				toRightsDto(image.getRights()));
	}

	public static IconSubjectDto toSubjectDto(IconSubject subject) {
		if (subject == null) {
			return null;
		}
		return new IconSubjectDto(
				subject.getId(),
				subject.getSubjectLabel(),
				subject.getFigureCertainty(),
				subject.isPrimary(),
				subject.getSortOrder(),
				SaintMapper.toSummary(subject.getSaint()));
	}

	public static ImageRightsDto toRightsDto(ImageRights rights) {
		if (rights == null) {
			return null;
		}
		return new ImageRightsDto(
				rights.getRightsHolder(),
				rights.getLicenseType(),
				rights.getLicenseUrl(),
				rights.getLicenseNotes(),
				rights.getAttributionText(),
				rights.getCopyrightYear(),
				rights.isVerified());
	}

	public static String depictedFigure(Icon icon) {
		IconSubject primary = primarySubject(icon);
		if (primary != null) {
			return primary.getSubjectLabel();
		}
		return subjectsInOrder(icon).stream()
				.map(IconSubject::getSubjectLabel)
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);
	}

	public static String contributorCredit(Icon icon) {
		if (icon.getAttributionPreference() == AttributionPreference.ANONYMOUS) {
			return ANONYMOUS_CREDIT;
		}
		String displayName = icon.getContributorDisplayName();
		if (isBlank(displayName) && icon.getContributor() != null) {
			displayName = icon.getContributor().getDisplayName();
		}
		return isBlank(displayName) ? ANONYMOUS_CREDIT : displayName;
	}

	private static List<String> saintNames(Icon icon) {
		return subjectsInOrder(icon).stream()
				.map(IconSubject::getSaint)
				.filter(Objects::nonNull)
				.map(Saint::getCanonicalName)
				.filter(name -> !isBlank(name))
				.distinct()
				.toList();
	}

	private static List<IconSubjectDto> subjects(Icon icon) {
		return subjectsInOrder(icon).stream().map(IconMapper::toSubjectDto).toList();
	}

	private static List<IconImageDto> images(Icon icon, Function<String, String> mediaUrlResolver) {
		return safeList(icon.getImages()).stream()
				.sorted(IMAGE_ORDER)
				.map(image -> toImageDto(image, mediaUrlResolver))
				.toList();
	}

	private static List<IconSubject> subjectsInOrder(Icon icon) {
		return safeList(icon.getSubjects()).stream().sorted(SUBJECT_ORDER).toList();
	}

	private static IconImage primaryImage(Icon icon) {
		List<IconImage> images = safeList(icon.getImages());
		if (images.isEmpty()) {
			return null;
		}
		return images.stream()
				.filter(IconImage::isPrimary)
				.findFirst()
				.orElseGet(() -> images.stream()
						.min(Comparator.comparingInt(IconImage::getSortOrder))
						.orElse(null));
	}

	private static IconSubject primarySubject(Icon icon) {
		List<IconSubject> subjects = subjectsInOrder(icon);
		if (subjects.isEmpty()) {
			return null;
		}
		return subjects.stream()
				.filter(IconSubject::isPrimary)
				.findFirst()
				.orElse(subjects.get(0));
	}

	private static <T> List<T> safeList(List<T> values) {
		return values == null ? List.of() : values;
	}

	private static String resolve(Function<String, String> mediaUrlResolver, String storageKey) {
		if (isBlank(storageKey) || mediaUrlResolver == null) {
			return null;
		}
		return mediaUrlResolver.apply(storageKey);
	}

	private static String firstNonBlank(String first, String second) {
		return isBlank(first) ? second : first;
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}
