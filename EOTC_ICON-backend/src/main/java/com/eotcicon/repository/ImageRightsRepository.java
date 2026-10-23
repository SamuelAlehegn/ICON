package com.eotcicon.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eotcicon.domain.ImageRights;

public interface ImageRightsRepository extends JpaRepository<ImageRights, UUID> {
}
