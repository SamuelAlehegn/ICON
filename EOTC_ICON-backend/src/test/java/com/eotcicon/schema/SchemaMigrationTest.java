package com.eotcicon.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the Flyway-managed schema (V2__create_core_schema.sql) on the
 * local H2 mirror: table set, the rules that the application depends on, and
 * the behaviour that must be identical on PostgreSQL in production.
 */
@SpringBootTest
@ActiveProfiles("local")
class SchemaMigrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void createsEveryCoreTable() {
		List<String> tables = jdbcTemplate.queryForList(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name",
				String.class);

		assertThat(tables).contains("audit_log", "church", "church_alias", "contribution_review", "icon", "icon_image",
				"icon_subject", "image_rights", "refresh_token", "saint", "saint_alias", "users");
	}

	@Test
	void allowsAnIconWithAnUnknownSaint() {
		UUID iconId = UUID.randomUUID();

		insertIcon(iconId, "icon-" + iconId, referenceCode(iconId));

		assertThatCode(() -> jdbcTemplate.update(
				"INSERT INTO icon_subject (id, icon_id, subject_label, figure_certainty) VALUES (?, ?, ?, 'UNKNOWN')",
				UUID.randomUUID(), iconId, "Unknown warrior"))
			.doesNotThrowAnyException();
	}

	@Test
	void rejectsAnUnidentifiedSubjectClaimingToBeIdentified() {
		UUID iconId = UUID.randomUUID();

		insertIcon(iconId, "icon-" + iconId, referenceCode(iconId));

		assertThatThrownBy(() -> jdbcTemplate.update(
				"INSERT INTO icon_subject (id, icon_id, subject_label, figure_certainty) VALUES (?, ?, ?, 'IDENTIFIED')",
				UUID.randomUUID(), iconId, "Saint George"))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsApprovalWithoutPublishedAt() {
		UUID iconId = UUID.randomUUID();

		insertIcon(iconId, "icon-" + iconId, referenceCode(iconId));

		assertThatThrownBy(() -> jdbcTemplate.update(
				"UPDATE icon SET publication_status = 'APPROVED' WHERE id = ?", iconId))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsADuplicateIconSlug() {
		UUID iconId = UUID.randomUUID();
		String slug = "icon-" + iconId;

		insertIcon(iconId, slug, referenceCode(iconId));

		assertThatThrownBy(() -> insertIcon(UUID.randomUUID(), slug, referenceCode(UUID.randomUUID())))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsAnUnknownPublicationStatus() {
		UUID iconId = UUID.randomUUID();

		insertIcon(iconId, "icon-" + iconId, referenceCode(iconId));

		assertThatThrownBy(() -> jdbcTemplate.update(
				"UPDATE icon SET publication_status = 'SOMETHING_ELSE' WHERE id = ?", iconId))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsOutOfRangeCoordinates() {
		assertThatThrownBy(() -> jdbcTemplate.update(
				"INSERT INTO church (id, name, slug, latitude) VALUES (?, ?, ?, ?)",
				UUID.randomUUID(), "Bad Church", "bad-church-" + UUID.randomUUID(), 91.0))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsAnImageWithAMalformedChecksum() {
		UUID iconId = UUID.randomUUID();

		insertIcon(iconId, "icon-" + iconId, referenceCode(iconId));

		assertThatThrownBy(() -> jdbcTemplate.update(
				"INSERT INTO icon_image (id, icon_id, storage_key, content_type, file_size_bytes, checksum_sha256) VALUES (?, ?, ?, ?, ?, ?)",
				UUID.randomUUID(), iconId, "icons/" + iconId + "/a.jpg", "image/jpeg", 1024, "too-short"))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsADuplicateUserEmail() {
		UUID userId = UUID.randomUUID();
		String email = "contributor-" + userId + "@eotc.org";

		jdbcTemplate.update("INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, ?, ?)",
				userId, email, "hash", "Contributor");

		assertThatThrownBy(() -> jdbcTemplate.update(
				"INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, ?, ?)",
				UUID.randomUUID(), email, "hash", "Impostor"))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void refusesToDeleteASaintThatIsStillDepicted() {
		UUID saintId = UUID.randomUUID();
		UUID iconId = UUID.randomUUID();

		jdbcTemplate.update("INSERT INTO saint (id, canonical_name, slug) VALUES (?, ?, ?)", saintId, "Saint George",
				"saint-george-" + saintId);
		insertIcon(iconId, "icon-" + iconId, referenceCode(iconId));
		jdbcTemplate.update(
				"INSERT INTO icon_subject (id, icon_id, saint_id, subject_label, figure_certainty) VALUES (?, ?, ?, ?, 'IDENTIFIED')",
				UUID.randomUUID(), iconId, saintId, "Saint George");

		assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM saint WHERE id = ?", saintId))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void cascadesDeletionToImagesSubjectsReviewsAndRights() {
		UUID iconId = UUID.randomUUID();
		UUID imageId = UUID.randomUUID();

		insertIcon(iconId, "icon-" + iconId, referenceCode(iconId));
		jdbcTemplate.update(
				"INSERT INTO icon_image (id, icon_id, storage_key, content_type, file_size_bytes, checksum_sha256) VALUES (?, ?, ?, ?, ?, ?)",
				imageId, iconId, "icons/" + iconId + "/a.jpg", "image/jpeg", 1024, "a".repeat(64));
		jdbcTemplate.update("INSERT INTO image_rights (id, icon_image_id, license_type) VALUES (?, ?, 'CC_BY')",
				UUID.randomUUID(), imageId);
		jdbcTemplate.update(
				"INSERT INTO contribution_review (id, icon_id, reviewer_id, action) VALUES (?, ?, ?, 'SUBMIT')",
				UUID.randomUUID(), iconId, userId());

		jdbcTemplate.update("DELETE FROM icon WHERE id = ?", iconId);

		assertThat(count("icon_image", "icon_id", iconId)).isZero();
		assertThat(count("icon_subject", "icon_id", iconId)).isZero();
		assertThat(count("contribution_review", "icon_id", iconId)).isZero();
		assertThat(count("image_rights", "icon_image_id", imageId)).isZero();
	}

	private void insertIcon(UUID iconId, String slug, String referenceCode) {
		jdbcTemplate.update(
				"INSERT INTO icon (id, title, slug, reference_code) VALUES (?, ?, ?, ?)",
				iconId, "Icon " + iconId, slug, referenceCode);
	}

	private static String referenceCode(UUID iconId) {
		return "EOTC-" + iconId.toString().substring(0, 12).toUpperCase();
	}

	private long count(String table, String column, UUID value) {
		Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM " + table + " WHERE " + column + " = ?",
				Long.class, value);
		return count == null ? 0L : count;
	}

	private UUID userId() {
		UUID userId = UUID.randomUUID();
		jdbcTemplate.update("INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, ?, ?)",
				userId, "reviewer-" + userId + "@eotc.org", "hash", "Reviewer");
		return userId;
	}

}
