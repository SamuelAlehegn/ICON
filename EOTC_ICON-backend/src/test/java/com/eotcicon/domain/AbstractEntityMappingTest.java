package com.eotcicon.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.eotcicon.domain.enums.AttributionPreference;
import com.eotcicon.domain.enums.FigureCertainty;
import com.eotcicon.domain.enums.LicenseType;
import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.domain.enums.RecordStatus;
import com.eotcicon.domain.enums.ReviewAction;
import com.eotcicon.domain.enums.UserRole;
import com.eotcicon.domain.enums.UserStatus;

/**
 * Contract between the JPA entities and the Flyway-managed schema.
 *
 * <p>
 * The assertions live in this abstract class so that they run twice: against
 * the H2 mirror ({@link EntityMappingTest}) and against PostgreSQL
 * ({@link EntityMappingPostgresIT}). Both engines therefore prove the same
 * mapping, and the schema validation performed by Hibernate on startup proves
 * that the mapping matches the DDL.
 */
@SpringBootTest
@Transactional
abstract class AbstractEntityMappingTest {

	private static final Instant BEFORE_EPOCH = Instant.parse("2000-01-01T00:00:00Z");

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void persistsAndReadsBackTheWholeIconAggregate() {
		UUID iconId = persistIconAggregate().getId();

		entityManager.clear();
		Icon icon = entityManager.find(Icon.class, iconId);

		assertThat(icon).isNotNull();
		assertThat(icon.getTitle()).isEqualTo("Icon of Saint George");
		assertThat(icon.getFigureCertainty()).isEqualTo(FigureCertainty.IDENTIFIED);
		assertThat(icon.getPublicationStatus()).isEqualTo(PublicationStatus.PENDING);
		assertThat(icon.getAttributionPreference()).isEqualTo(AttributionPreference.ANONYMOUS);
		assertThat(icon.getContactEmail()).isEqualTo("private@example.org");
		assertThat(icon.getCaptureDate()).isEqualTo(LocalDate.of(2024, 3, 17));
		assertThat(icon.getArtworkDateStart()).isEqualTo(LocalDate.of(1890, 1, 1));
		assertThat(icon.getChurch().getName()).isEqualTo("St. George Cathedral");
		assertThat(icon.getChurch().getLatitude()).isEqualByComparingTo(new BigDecimal("9.019200"));
		assertThat(icon.getContributor().getDisplayName()).isEqualTo("Contributor");
		assertThat(icon.getSubjects()).singleElement().satisfies(subject -> {
			assertThat(subject.getSubjectLabel()).isEqualTo("Saint George");
			assertThat(subject.getFigureCertainty()).isEqualTo(FigureCertainty.IDENTIFIED);
			assertThat(subject.isPrimary()).isTrue();
			assertThat(subject.getSaint().getCanonicalName()).isEqualTo("Saint George");
		});
		assertThat(icon.getImages()).singleElement().satisfies(image -> {
			assertThat(image.getContentType()).isEqualTo("image/jpeg");
			assertThat(image.getFileSizeBytes()).isEqualTo(2048L);
			assertThat(image.getChecksumSha256()).hasSize(64);
			assertThat(image.getAltText()).isEqualTo("Saint George slaying the dragon");
			assertThat(image.getRights().getLicenseType()).isEqualTo(LicenseType.CC_BY);
			assertThat(image.getRights().getCopyrightYear()).isEqualTo(1890);
		});
		assertThat(icon.getReviews()).singleElement().satisfies(review -> {
			assertThat(review.getAction()).isEqualTo(ReviewAction.SUBMIT);
			assertThat(review.getToStatus()).isEqualTo(PublicationStatus.PENDING);
			assertThat(review.getReviewer().getDisplayName()).isEqualTo("Contributor");
		});
	}

	@Test
	void stampsTheAuditColumnsOnInsertAndRefreshesThemOnUpdate() {
		Icon icon = persistIconAggregate();
		UUID iconId = icon.getId();

		assertThat(icon.getId()).isNotNull();
		assertThat(icon.getCreatedAt()).isNotNull();
		assertThat(icon.getUpdatedAt()).isNotNull();

		entityManager.clear();
		jdbcTemplate.update("UPDATE icon SET updated_at = ? WHERE id = ?",
				OffsetDateTime.ofInstant(BEFORE_EPOCH, ZoneOffset.UTC), iconId);

		entityManager.clear();
		Icon reloaded = entityManager.find(Icon.class, iconId);
		reloaded.setTitle("Icon of Saint George (revised)");
		entityManager.flush();
		entityManager.clear();

		Icon updated = entityManager.find(Icon.class, iconId);
		assertThat(updated.getTitle()).isEqualTo("Icon of Saint George (revised)");
		assertThat(updated.getCreatedAt()).isNotNull();
		assertThat(updated.getUpdatedAt()).isAfter(BEFORE_EPOCH);
	}

	@Test
	void acceptsEveryEnumValueOfTheCheckConstraints() {
		for (UserRole role : UserRole.values()) {
			entityManager.persist(user(role, UserStatus.ACTIVE));
		}
		for (UserStatus status : UserStatus.values()) {
			entityManager.persist(user(UserRole.CONTRIBUTOR, status));
		}
		for (RecordStatus status : RecordStatus.values()) {
			entityManager.persist(church(status));
		}
		for (FigureCertainty certainty : FigureCertainty.values()) {
			entityManager.persist(icon(certainty, AttributionPreference.DISPLAY_NAME, PublicationStatus.PENDING));
		}
		for (AttributionPreference preference : AttributionPreference.values()) {
			entityManager.persist(icon(FigureCertainty.UNKNOWN, preference, PublicationStatus.PENDING));
		}
		for (PublicationStatus status : PublicationStatus.values()) {
			entityManager.persist(icon(FigureCertainty.UNKNOWN, AttributionPreference.DISPLAY_NAME, status));
		}
		for (ReviewAction action : ReviewAction.values()) {
			entityManager.persist(review(action));
		}
		for (LicenseType licenseType : LicenseType.values()) {
			entityManager.persist(imageRights(licenseType));
		}

		entityManager.flush();
	}

	private Icon persistIconAggregate() {
		User contributor = user(UserRole.CONTRIBUTOR, UserStatus.ACTIVE);

		Church church = church(RecordStatus.ACTIVE);
		ChurchAlias churchAlias = new ChurchAlias();
		churchAlias.setChurch(church);
		churchAlias.setName("Kidist Selassie");
		entityManager.persist(churchAlias);

		Saint saint = new Saint();
		saint.setCanonicalName("Saint George");
		saint.setSlug("saint-george-" + UUID.randomUUID());
		saint.setAmharicName("ጊዮርጊስ");
		saint.setCategory("Warrior saint");
		saint.setDescription("A warrior saint, depicted with a dragon.");
		saint.setVerified(true);
		entityManager.persist(saint);

		SaintAlias saintAlias = new SaintAlias();
		saintAlias.setSaint(saint);
		saintAlias.setName("Saint Giorgis");
		entityManager.persist(saintAlias);

		Icon icon = icon(FigureCertainty.IDENTIFIED, AttributionPreference.ANONYMOUS, PublicationStatus.PENDING);
		icon.setChurch(church);
		icon.setContributor(contributor);
		icon.setContributorDisplayName("Contributor");
		icon.setDescription("Painted panel icon.");
		icon.setHistoricalNotes("Nineteenth century, Gondarine style.");
		icon.setCategory("Panel");
		icon.setCaptureDate(LocalDate.of(2024, 3, 17));
		icon.setArtworkDateStart(LocalDate.of(1890, 1, 1));
		icon.setArtworkDateEnd(LocalDate.of(1900, 1, 1));
		icon.setContactEmail("private@example.org");
		entityManager.persist(icon);

		IconSubject subject = new IconSubject();
		subject.setIcon(icon);
		subject.setSaint(saint);
		subject.setSubjectLabel("Saint George");
		subject.setFigureCertainty(FigureCertainty.IDENTIFIED);
		subject.setPrimary(true);
		entityManager.persist(subject);

		IconImage image = iconImage();
		image.setIcon(icon);
		image.setUploadedBy(contributor);
		entityManager.persist(image);

		ImageRights rights = imageRights(LicenseType.CC_BY);
		rights.setImage(image);
		rights.setVerifiedBy(contributor);
		rights.setVerifiedAt(Instant.now());
		image.setRights(rights);
		entityManager.persist(rights);

		ContributionReview review = review(ReviewAction.SUBMIT);
		review.setIcon(icon);
		review.setReviewer(contributor);
		entityManager.persist(review);

		RefreshToken token = new RefreshToken();
		token.setUser(contributor);
		token.setTokenHash("hash-" + UUID.randomUUID());
		token.setExpiresAt(Instant.now().plusSeconds(3600));
		entityManager.persist(token);

		AuditLog auditLog = new AuditLog();
		auditLog.setActor(contributor);
		auditLog.setAction("ICON_CREATED");
		auditLog.setEntityType("icon");
		auditLog.setEntityId(icon.getId());
		auditLog.setDetail("{\"source\":\"web\"}");
		auditLog.setIpAddress("127.0.0.1");
		auditLog.setUserAgent("JUnit");
		entityManager.persist(auditLog);

		entityManager.flush();
		return icon;
	}

	private User user(UserRole role, UserStatus status) {
		User user = new User();
		user.setEmail("user-" + UUID.randomUUID() + "@eotc.org");
		user.setPasswordHash("hash");
		user.setDisplayName("Contributor");
		user.setRole(role);
		user.setStatus(status);
		entityManager.persist(user);
		return user;
	}

	private Church church(RecordStatus status) {
		Church church = new Church();
		church.setName("St. George Cathedral");
		church.setSlug("st-george-cathedral-" + UUID.randomUUID());
		church.setRegion("Addis Ababa");
		church.setTown("Addis Ababa");
		church.setLatitude(new BigDecimal("9.019200"));
		church.setLongitude(new BigDecimal("38.752500"));
		church.setDescription("A round church built in the nineteenth century.");
		church.setStatus(status);
		entityManager.persist(church);
		return church;
	}

	private Icon icon(FigureCertainty certainty, AttributionPreference preference, PublicationStatus status) {
		Icon icon = new Icon();
		icon.setTitle("Icon of Saint George");
		icon.setSlug("icon-of-saint-george-" + UUID.randomUUID());
		icon.setReferenceCode("EOTC-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
		icon.setFigureCertainty(certainty);
		icon.setAttributionPreference(preference);
		icon.setPublicationStatus(status);
		if (status == PublicationStatus.APPROVED) {
			icon.setPublishedAt(Instant.now());
		}
		entityManager.persist(icon);
		return icon;
	}

	private IconImage iconImage() {
		IconImage image = new IconImage();
		image.setStorageKey("icons/" + UUID.randomUUID() + "/main.jpg");
		image.setThumbnailStorageKey("icons/" + UUID.randomUUID() + "/thumb.jpg");
		image.setContentType("image/jpeg");
		image.setFileSizeBytes(2048L);
		image.setChecksumSha256("a".repeat(64));
		image.setWidthPx(1024);
		image.setHeightPx(768);
		image.setAltText("Saint George slaying the dragon");
		image.setPrimary(true);
		image.setCapturedAt(Instant.now());
		return image;
	}

	private ImageRights imageRights(LicenseType licenseType) {
		IconImage image = iconImage();
		image.setIcon(icon(FigureCertainty.UNKNOWN, AttributionPreference.DISPLAY_NAME, PublicationStatus.PENDING));
		entityManager.persist(image);

		ImageRights rights = new ImageRights();
		rights.setImage(image);
		rights.setRightsHolder("Ethiopian Orthodox Church");
		rights.setLicenseType(licenseType);
		rights.setLicenseUrl("https://creativecommons.org/licenses/by/4.0/");
		rights.setAttributionText("Photo by a contributor");
		rights.setCopyrightYear(1890);
		rights.setVerified(true);
		entityManager.persist(rights);
		return rights;
	}

	private ContributionReview review(ReviewAction action) {
		ContributionReview review = new ContributionReview();
		review.setIcon(icon(FigureCertainty.UNKNOWN, AttributionPreference.DISPLAY_NAME, PublicationStatus.PENDING));
		review.setReviewer(user(UserRole.MODERATOR, UserStatus.ACTIVE));
		review.setAction(action);
		review.setFromStatus(PublicationStatus.PENDING);
		review.setToStatus(PublicationStatus.PENDING);
		review.setNotes("Submitted by a contributor");
		entityManager.persist(review);
		return review;
	}

}
