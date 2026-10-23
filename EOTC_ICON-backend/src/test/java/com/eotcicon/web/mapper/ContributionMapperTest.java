package com.eotcicon.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.eotcicon.domain.Church;
import com.eotcicon.domain.ContributionReview;
import com.eotcicon.domain.Icon;
import com.eotcicon.domain.IconImage;
import com.eotcicon.domain.IconSubject;
import com.eotcicon.domain.Saint;
import com.eotcicon.domain.User;
import com.eotcicon.domain.enums.AttributionPreference;
import com.eotcicon.domain.enums.FigureCertainty;
import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.domain.enums.ReviewAction;
import com.eotcicon.web.dto.contribution.AdminSubmissionDetailDto;
import com.eotcicon.web.dto.contribution.ContributionCreatedDto;
import com.eotcicon.web.dto.contribution.ContributionStatusDto;
import com.eotcicon.web.dto.contribution.ReviewHistoryDto;
import com.eotcicon.web.dto.contribution.SubmissionSummaryDto;

class ContributionMapperTest {

	private static final Function<String, String> URLS = key -> "https://cdn.example/" + key;
	private static final Instant EARLIER = Instant.parse("2024-01-01T00:00:00Z");
	private static final Instant LATER = Instant.parse("2024-01-02T00:00:00Z");

	@Test
	void mapsTheContributionReceiptAndAnonymousStatus() {
		Icon icon = icon();

		ContributionCreatedDto created = ContributionMapper.toCreated(icon, "Thank you");
		ContributionStatusDto status = ContributionMapper.toStatus(icon, "Awaiting review");

		assertThat(created.referenceCode()).isEqualTo("EOTC-ABCDEF123456");
		assertThat(created.status()).isEqualTo(PublicationStatus.PENDING);
		assertThat(created.message()).isEqualTo("Thank you");
		assertThat(status.referenceCode()).isEqualTo("EOTC-ABCDEF123456");
		assertThat(status.title()).isEqualTo("Saint George");
		assertThat(status.churchName()).isEqualTo("Bete Giyorgis");
		assertThat(status.depictedFigure()).isEqualTo("Saint George");
		assertThat(status.imageCount()).isEqualTo(1);
		assertThat(status.moderatorNotes()).isEqualTo("Awaiting review");
	}

	@Test
	void mapsTheModerationSummary() {
		SubmissionSummaryDto summary = ContributionMapper.toSummary(icon());

		assertThat(summary.referenceCode()).isEqualTo("EOTC-ABCDEF123456");
		assertThat(summary.title()).isEqualTo("Saint George");
		assertThat(summary.depictedFigure()).isEqualTo("Saint George");
		assertThat(summary.churchName()).isEqualTo("Bete Giyorgis");
		assertThat(summary.status()).isEqualTo(PublicationStatus.PENDING);
		assertThat(summary.imageCount()).isEqualTo(1);
	}

	@Test
	void adminDetailIncludesThePrivateEmailAndSortedReviewHistory() {
		Icon icon = icon();

		AdminSubmissionDetailDto detail = ContributionMapper.toAdminDetail(icon, URLS);

		assertThat(detail.contactEmail()).isEqualTo("private@example.org");
		assertThat(detail.contributorDisplayName()).isEqualTo("Ezana");
		assertThat(detail.contributorCredit()).isEqualTo("Ezana");
		assertThat(detail.attributionPreference()).isEqualTo(AttributionPreference.DISPLAY_NAME);
		assertThat(detail.images()).hasSize(1);
		assertThat(detail.images().get(0).imageUrl()).isEqualTo("https://cdn.example/icons/1/full.jpg");
		assertThat(detail.reviews()).hasSize(2);
		assertThat(detail.reviews()).extracting(ReviewHistoryDto::action)
				.containsExactly(ReviewAction.SUBMIT, ReviewAction.REQUEST_CORRECTION);
		assertThat(detail.reviews().get(1).reviewerDisplayName()).isEqualTo("Moderator");
	}

	@Test
	void publicContributionDtosHaveNoPrivateContactField() {
		assertThat(ContributionCreatedDto.class.getRecordComponents())
				.extracting(component -> component.getName())
				.doesNotContain("contactEmail", "moderationNotes");
		assertThat(ContributionStatusDto.class.getRecordComponents())
				.extracting(component -> component.getName())
				.doesNotContain("contactEmail");
		assertThat(SubmissionSummaryDto.class.getRecordComponents())
				.extracting(component -> component.getName())
				.doesNotContain("contactEmail");
	}

	private static Icon icon() {
		User contributor = new User();
		contributor.setEmail("contributor@example.org");
		contributor.setDisplayName("Ezana");
		contributor.setPasswordHash("hash");

		User moderator = new User();
		ReflectionTestUtils.setField(moderator, "id", UUID.randomUUID());
		moderator.setEmail("moderator@example.org");
		moderator.setDisplayName("Moderator");
		moderator.setPasswordHash("hash");

		Church church = new Church();
		church.setName("Bete Giyorgis");
		church.setSlug("bete-giyorgis");
		church.setTown("Lalibela");
		church.setRegion("Amhara");
		church.setCountry("Ethiopia");

		Saint saint = new Saint();
		saint.setCanonicalName("Saint George");
		saint.setSlug("saint-george");

		Icon icon = new Icon();
		ReflectionTestUtils.setField(icon, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(icon, "createdAt", EARLIER);
		ReflectionTestUtils.setField(icon, "updatedAt", LATER);
		icon.setReferenceCode("EOTC-ABCDEF123456");
		icon.setSlug("saint-george");
		icon.setTitle("Saint George");
		icon.setDescription("A panel icon.");
		icon.setFigureCertainty(FigureCertainty.IDENTIFIED);
		icon.setPublicationStatus(PublicationStatus.PENDING);
		icon.setAttributionPreference(AttributionPreference.DISPLAY_NAME);
		icon.setContributorDisplayName("Ezana");
		icon.setContactEmail("private@example.org");
		icon.setContributor(contributor);
		icon.setChurch(church);

		IconSubject subject = new IconSubject();
		subject.setIcon(icon);
		subject.setSaint(saint);
		subject.setSubjectLabel("Saint George");
		subject.setFigureCertainty(FigureCertainty.IDENTIFIED);
		subject.setPrimary(true);
		icon.getSubjects().add(subject);

		IconImage image = new IconImage();
		image.setIcon(icon);
		image.setStorageKey("icons/1/full.jpg");
		image.setThumbnailStorageKey("icons/1/thumb.jpg");
		image.setAltText("Saint George");
		image.setContentType("image/jpeg");
		image.setFileSizeBytes(1024);
		image.setPrimary(true);
		icon.getImages().add(image);

		ContributionReview submit = new ContributionReview();
		ReflectionTestUtils.setField(submit, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(submit, "createdAt", EARLIER);
		submit.setIcon(icon);
		submit.setReviewer(contributor);
		submit.setAction(ReviewAction.SUBMIT);
		submit.setToStatus(PublicationStatus.PENDING);
		icon.getReviews().add(submit);

		ContributionReview correction = new ContributionReview();
		ReflectionTestUtils.setField(correction, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(correction, "createdAt", LATER);
		correction.setIcon(icon);
		correction.setReviewer(moderator);
		correction.setAction(ReviewAction.REQUEST_CORRECTION);
		correction.setFromStatus(PublicationStatus.PENDING);
		correction.setToStatus(PublicationStatus.NEEDS_CORRECTION);
		correction.setNotes("Please add the church town.");
		icon.getReviews().add(correction);

		return icon;
	}

}
