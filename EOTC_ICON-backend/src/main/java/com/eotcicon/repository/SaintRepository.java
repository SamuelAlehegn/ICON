package com.eotcicon.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eotcicon.domain.Saint;
import com.eotcicon.domain.enums.RecordStatus;

public interface SaintRepository extends JpaRepository<Saint, UUID> {

	Optional<Saint> findBySlug(String slug);

	boolean existsBySlug(String slug);

	List<Saint> findAllByStatus(RecordStatus status);

	long countByStatus(RecordStatus status);
}
