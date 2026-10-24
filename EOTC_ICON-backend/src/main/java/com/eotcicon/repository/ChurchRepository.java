package com.eotcicon.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.eotcicon.domain.Church;
import com.eotcicon.domain.enums.RecordStatus;

public interface ChurchRepository extends JpaRepository<Church, UUID> {

	Optional<Church> findBySlug(String slug);

	boolean existsBySlug(String slug);

	List<Church> findAllByStatus(RecordStatus status);

	long countByStatus(RecordStatus status);

	@Query("SELECT COUNT(DISTINCT c.region) FROM Church c WHERE c.status = :status AND c.region IS NOT NULL AND c.region <> ''")
	long countDistinctRegionsByStatus(RecordStatus status);
}
