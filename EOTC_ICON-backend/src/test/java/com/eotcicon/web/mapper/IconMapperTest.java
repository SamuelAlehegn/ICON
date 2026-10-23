package com.eotcicon.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.eotcicon.domain.Church;
import com.eotcicon.domain.Icon;
import com.eotcicon.domain.IconImage;
import com.eotcicon.domain.IconSubject;
import com.eotcicon.domain.ImageRights;
import com.eotcicon.domain.Saint;
import com.eotcicon.domain.enums.AttributionPreference;
import com.eotcicon.domain.enums.FigureCertainty;
import com.eotcicon.domain.enums.LicenseType;
import com.eotcicon.web.dto.common.PageResponse;
import com.eotcicon.web.dto.icon.IconDetailDto;
import com.eotcicon.web.dto.icon.IconSummaryDto;

class IconMapperTest {

	private static final UUID ICON_ID = UUID.randomUUID();
	private static final UUID CHURCH_ID = UUID.randomUUID();
	private static final UUID SAINT_ID = UUID.randomUUID();
	private static final Function<String, String> URLS = key -> "https://cdn.example/" + key;

	@Test
	void mapsTheGalleryCardFromThePrimarySubjectAndImage() {
		Icon icon = icon();

		IconSummaryDto dto = IconMapper.toSummary(icon, URLS);

		assertThat(dto.id()).isEqualTo(ICON_ID);
		assertThat(dto.slug()).isEqualTo("saint-george-on-horseback");
		assertThat(dto.title()).isEqualTo("Saint George on horseback");
		assertThat(dto.depictedFigure()).isEqualTo("Saint George");
		assertThat(dto.figureCertainty()).isEqualTo(FigureCertainty.IDENTIFIED);
		assertThat(dto.saintNames()).containsExactly("Saint George", "Saint Mercurius");
		assertThat(dto.churchName()).isEqualTo("Bete Giyorgis");
		assertThat(dto.churchSlug()).isEqualTo("bete-giyorgis");
		assertThat(dto.town()).isEqualTo("Lalibela");
		assertThat(dto.region()).isEqualTo("Amhara");
		assertThat(dto.country()).isEqualTo("Ethiopia");
		assertThat(dto.contributorCredit()).isEqualTo("Ezana");
		assertThat(dto.thumbnailUrl()).isEqualTo("https://cdn.example/icons/1/thumb.jpg");
		assertThat(dto.imageUrl()).isEqualTo("https://cdn.example/icons/1/full.jpg");
		assertThat(dto.altText()).isEqualTo("Saint George at Bete Giyorgis");
	}

	@Test
	void honoursTheAttributionPreference() {
		Icon anonymous = icon();
		anonymous.setAttributionPreference(AttributionPreference.ANONYMOUS);

		assertThat(IconMapper.toSummary(anonymous, URLS).contributorCredit())
				.isEqualTo("Anonymous contributor");
	}

	@Test
	void fallsBackToTheLowestSortOrderWhenNoImageIsMarkedPrimary() {
		Icon icon = icon();
		icon.getImages().forEach(image -> image.setPrimary(false));
		icon.getImages().get(0).setSortOrder(2);
		icon.getImages().get(1).setSortOrder(0);

		IconSummaryDto dto = IconMapper.toSummary(icon, URLS);

		assertThat(dto.imageUrl()).isEqualTo("https://cdn.example/icons/1/primary.jpg");
	}

	@Test
	void mapsIconDetailWithSubjectsImagesAndRights() {
		Icon icon = icon();
		IconSummaryDto related = IconMapper.toSummary(icon(), URLS);
		IconSummaryDto sameChurch = IconMapper.toSummary(icon(), URLS);

		IconDetailDto detail = IconMapper.toDetail(icon, URLS, List.of(related), List.of(sameChurch));

		assertThat(detail.id()).isEqualTo(ICON_ID);
		assertThat(detail.slug()).isEqualTo("saint-george-on-horseback");
		assertThat(detail.referenceCode()).isEqualTo("EOTC-ABCDEF123456");
		assertThat(detail.figureCertainty()).isEqualTo(FigureCertainty.IDENTIFIED);
		assertThat(detail.captureDate()).isEqualTo(icon.getCaptureDate());
		assertThat(detail.artworkDateStart()).isEqualTo(icon.getArtworkDateStart());
		assertThat(detail.contributorCredit()).isEqualTo("Ezana");
		assertThat(detail.church().name()).isEqualTo("Bete Giyorgis");
		assertThat(detail.subjects()).hasSize(2);
		assertThat(detail.subjects().get(0).saint().canonicalName()).isEqualTo("Saint George");
		assertThat(detail.images()).hasSize(2);
		assertThat(detail.images().get(0).primary()).isTrue();
		assertThat(detail.images().get(0).rights().licenseType()).isEqualTo(LicenseType.CC_BY);
		assertThat(detail.images().get(0).rights().verified()).isTrue();
		assertThat(detail.relatedIcons()).containsExactly(related);
		assertThat(detail.moreFromChurch()).containsExactly(sameChurch);
	}

	@Test
	void summaryPageCarriesThePaginationEnvelope() {
		Icon icon = icon();
		PageImpl<Icon> page = new PageImpl<>(List.of(icon), PageRequest.of(0, 12), 1);

		PageResponse<IconSummaryDto> response = IconMapper.toSummaryPage(page, URLS);

		assertThat(response.content()).hasSize(1);
		assertThat(response.page()).isZero();
		assertThat(response.size()).isEqualTo(12);
		assertThat(response.totalElements()).isEqualTo(1);
		assertThat(response.totalPages()).isEqualTo(1);
		assertThat(response.first()).isTrue();
		assertThat(response.last()).isTrue();
	}

	@Test
	void publicDtosHaveNoPrivateFields() {
		assertThat(IconSummaryDto.class.getRecordComponents())
				.extracting(component -> component.getName())
				.doesNotContain("contactEmail", "moderationNotes", "contributor");
		assertThat(IconDetailDto.class.getRecordComponents())
				.extracting(component -> component.getName())
				.doesNotContain("contactEmail", "moderationNotes", "contributor");
	}

	private static Icon icon() {
		Church church = new Church();
		ReflectionTestUtils.setField(church, "id", CHURCH_ID);
		church.setName("Bete Giyorgis");
		church.setSlug("bete-giyorgis");
		church.setTown("Lalibela");
		church.setRegion("Amhara");
		church.setCountry("Ethiopia");

		Saint george = saint("Saint George", "saint-george");
		Saint mercurius = saint("Saint Mercurius", "saint-mercurius");

		Icon icon = new Icon();
		ReflectionTestUtils.setField(icon, "id", ICON_ID);
		ReflectionTestUtils.setField(icon, "createdAt", Instant.parse("2024-01-01T00:00:00Z"));
		ReflectionTestUtils.setField(icon, "updatedAt", Instant.parse("2024-01-02T00:00:00Z"));
		icon.setTitle("Saint George on horseback");
		icon.setSlug("saint-george-on-horseback");
		icon.setReferenceCode("EOTC-ABCDEF123456");
		icon.setDescription("A painted panel icon.");
		icon.setHistoricalNotes("Gondarine style.");
		icon.setCategory("Panel");
		icon.setFigureCertainty(FigureCertainty.IDENTIFIED);
		icon.setContributorDisplayName("Ezana");
		icon.setAttributionPreference(AttributionPreference.DISPLAY_NAME);
		icon.setContactEmail("private@example.org");
		icon.setPublishedAt(Instant.parse("2024-02-01T00:00:00Z"));
		icon.setChurch(church);

		IconSubject primarySubject = new IconSubject();
		primarySubject.setIcon(icon);
		primarySubject.setSaint(george);
		primarySubject.setSubjectLabel("Saint George");
		primarySubject.setFigureCertainty(FigureCertainty.IDENTIFIED);
		primarySubject.setPrimary(true);
		primarySubject.setSortOrder(0);
		icon.getSubjects().add(primarySubject);

		IconSubject secondarySubject = new IconSubject();
		secondarySubject.setIcon(icon);
		secondarySubject.setSaint(mercurius);
		secondarySubject.setSubjectLabel("Saint Mercurius");
		secondarySubject.setFigureCertainty(FigureCertainty.PROBABLE);
		secondarySubject.setPrimary(false);
		secondarySubject.setSortOrder(1);
		icon.getSubjects().add(secondarySubject);

		IconImage primaryImage = image("icons/1/full.jpg", "icons/1/thumb.jpg",
				"Saint George at Bete Giyorgis", true, 0);
		primaryImage.setIcon(icon);
		ImageRights rights = new ImageRights();
		rights.setRightsHolder("Bete Giyorgis");
		rights.setLicenseType(LicenseType.CC_BY);
		rights.setLicenseUrl("https://creativecommons.org/licenses/by/4.0/");
		rights.setAttributionText("Photo: Bete Giyorgis");
		rights.setCopyrightYear(2024);
		rights.setVerified(true);
		rights.setImage(primaryImage);
		primaryImage.setRights(rights);
		icon.getImages().add(primaryImage);

		IconImage secondaryImage = image("icons/1/primary.jpg", "icons/1/primary-thumb.jpg",
				"Reverse of the icon", false, 1);
		secondaryImage.setIcon(icon);
		icon.getImages().add(secondaryImage);

		return icon;
	}

	private static Saint saint(String canonicalName, String slug) {
		Saint saint = new Saint();
		ReflectionTestUtils.setField(saint, "id", SAINT_ID);
		saint.setCanonicalName(canonicalName);
		saint.setSlug(slug);
		return saint;
	}

	private static IconImage image(String storageKey, String thumbnailKey, String altText, boolean primary,
			int sortOrder) {
		IconImage image = new IconImage();
		image.setStorageKey(storageKey);
		image.setThumbnailStorageKey(thumbnailKey);
		image.setContentType("image/jpeg");
		image.setFileSizeBytes(2048);
		image.setWidthPx(1200);
		image.setHeightPx(1600);
		image.setAltText(altText);
		image.setPrimary(primary);
		image.setSortOrder(sortOrder);
		image.setCapturedAt(Instant.parse("2024-01-01T00:00:00Z"));
		return image;
	}

}
