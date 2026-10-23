package com.eotcicon.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eotcicon.domain.Church;
import com.eotcicon.domain.enums.RecordStatus;

public interface ChurchRepository extends JpaRepository<Church, UUID> {

	Optional<Church> findBySlug(String slug);

	boolean existsBySlug(String slug);
}
