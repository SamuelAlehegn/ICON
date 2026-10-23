package com.eotcicon.web.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import com.eotcicon.domain.ContributionReview;
import com.eotcicon.domain.Icon;
import com.eotcicon.domain.IconImage;
import com.eotcicon.domain.IconSubject;
import com.eotcicon.web.dto.church.ChurchSummaryDto;
import com.eotcicon.web.dto.contribution.AdminSubmissionDetailDto;
import com.eotcicon.web.dto.contribution.ContributionCreatedDto;
import com.eotcicon.web.dto.contribution.ContributionStatusDto;
import com.eotcicon.web.dto.contribution.ReviewHistoryDto;
import com.eotcicon.web.dto.contribution.SubmissionSummaryDto;
import com.eotcicon.web.dto.icon.IconImageDto;
import com.eotcicon.web.dto.icon.IconSubjectDto;

/**
 * Manual mapping for contribution submissions and the moderation queue.
 */
public final class ContributionMapper {

	private ContributionMapper() {
	}

	public static ContributionCreatedDto toCreated(Icon icon, String message) {
		return new ContributionCreatedDto(icon.getReferenceCode(), icon.getPublicationStatus(), message);
	}

	public static ContributionStatusDto toStatus(Icon icon, String moderatorNotes) {
		return new ContributionStatusDto(
				icon.getReferenceCode(),
				icon.getPublicationStatus(),
				icon.getTitle(),
				churchName(icon),
				IconMapper.depictedFigure(icon),
				imageCount(icon),
				icon.getCreatedAt(),
				icon.getUpdatedAt(),
				moderatorNotes);
	}

	public static SubmissionSummaryDto toSummary(Icon icon) {
		return new SubmissionSummaryDto(
				icon.getId(),
				icon.getReferenceCode(),
				icon.getTitle(),
				IconMapper.depictedFigure(icon),
				churchName(icon),
				icon.getPublicationStatus(),
				imageCount(icon),
				icon.getCreatedAt(),
				icon.getUpdatedAt());
	}

	public static AdminSubmissionDetailDto toAdminDetail(Icon icon, Function<String, String> mediaUrlResolver) {
		ChurchSummaryDto church = ChurchMapper.toSummary(icon.getChurch());
		List<IconSubjectDto> subjects = safeSubjects(icon).stream()
				.map(IconMapper::toSubjectDto)
				.toList();
		List<IconImageDto> images = safeImages(icon).stream()
				.map(image -> IconMapper.toImageDto(image, mediaUrlResolver))
				.toList();
		List<ReviewHistoryDto> reviews = safeReviews(icon).stream()
				.sorted(Comparator.comparing(ContributionReview::getCreatedAt,
						Comparator.nullsLast(Comparator.naturalOrder())))
				.map(ContributionMapper::toReview)
				.toList();
		return new AdminSubmissionDetailDto(
				icon.getId(),
				icon.getReferenceCode(),
				icon.getSlug(),
				icon.getTitle(),
				icon.getPublicationStatus(),
				icon.getDescription(),
				icon.getHistoricalNotes(),
				icon.getCategory(),
				icon.getFigureCertainty(),
				icon.getCaptureDate(),
				icon.getArtworkDateStart(),
				icon.getArtworkDateEnd(),
				icon.getContributorDisplayName(),
				icon.getAttributionPreference(),
				IconMapper.contributorCredit(icon),
				icon.getContactEmail(),
				icon.getPublishedAt(),
				church,
				subjects,
				images,
				reviews,
				icon.getCreatedAt(),
				icon.getUpdatedAt());
	}

	public static ReviewHistoryDto toReview(ContributionReview review) {
		if (review == null) {
			return null;
		}
		return new ReviewHistoryDto(
				review.getId(),
				review.getAction(),
				review.getFromStatus(),
				review.getToStatus(),
				review.getNotes(),
				review.getReviewer() == null ? null : review.getReviewer().getId(),
				review.getReviewer() == null ? null : review.getReviewer().getDisplayName(),
				review.getCreatedAt());
	}

	private static String churchName(Icon icon) {
		return icon.getChurch() == null ? null : icon.getChurch().getName();
	}

	private static int imageCount(Icon icon) {
		return safeImages(icon).size();
	}

	private static List<IconSubject> safeSubjects(Icon icon) {
		return icon.getSubjects() == null ? List.of() : icon.getSubjects();
	}

	private static List<IconImage> safeImages(Icon icon) {
		return icon.getImages() == null ? List.of() : icon.getImages();
	}

	private static List<ContributionReview> safeReviews(Icon icon) {
		return icon.getReviews() == null ? List.of() : icon.getReviews();
	}

}
