package com.eotcicon.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eotcicon.domain.Icon;
import com.eotcicon.domain.enums.PublicationStatus;

public interface IconRepository extends JpaRepository<Icon, UUID> {

	Optional<Icon> findBySlug(String slug);

	Optional<Icon> findByReferenceCode(String referenceCode);

	boolean existsBySlug(String slug);

	boolean existsByReferenceCode(String referenceCode);

	Page<Icon> findByPublicationStatus(PublicationStatus publicationStatus, Pageable pageable);

	Page<Icon> findByPublicationStatusAndChurch_Id(PublicationStatus publicationStatus, UUID churchId, Pageable pageable);

	Page<Icon> findByPublicationStatusAndCategoryIgnoreCase(PublicationStatus publicationStatus, String category, Pageable pageable);

	@Query("SELECT DISTINCT i FROM Icon i " +
			"LEFT JOIN i.subjects s " +
			"LEFT JOIN s.saint st " +
			"WHERE i.publicationStatus = :status " +
			"AND (LOWER(i.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
			"OR LOWER(COALESCE(i.description, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
			"OR LOWER(COALESCE(i.historicalNotes, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
			"OR LOWER(COALESCE(s.subjectLabel, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
			"OR LOWER(COALESCE(st.canonicalName, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
			"OR LOWER(COALESCE(st.amharicName, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
			"OR LOWER(COALESCE(st.geezName, '')) LIKE LOWER(CONCAT('%', :q, '%')))")
	Page<Icon> searchPublished(@Param("status") PublicationStatus status, @Param("q") String query, Pageable pageable);

	@Query("SELECT i FROM Icon i WHERE i.publicationStatus = :status " +
			"AND (:churchId IS NULL OR i.church.id = :churchId) " +
			"AND (:category IS NULL OR LOWER(i.category) = LOWER(:category)) " +
			"AND (:saintId IS NULL OR EXISTS (SELECT 1 FROM IconSubject sub WHERE sub.icon = i AND sub.saint.id = :saintId))")
	Page<Icon> filterPublished(@Param("status") PublicationStatus status,
			@Param("churchId") UUID churchId,
			@Param("category") String category,
			@Param("saintId") UUID saintId,
			Pageable pageable);
}
