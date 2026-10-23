package com.eotcicon.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eotcicon.domain.Saint;

public interface SaintRepository extends JpaRepository<Saint, UUID> {

	Optional<Saint> findBySlug(String slug);

	boolean existsBySlug(String slug);
}
