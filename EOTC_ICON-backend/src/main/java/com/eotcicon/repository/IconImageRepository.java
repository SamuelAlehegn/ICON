package com.eotcicon.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eotcicon.domain.IconImage;

public interface IconImageRepository extends JpaRepository<IconImage, UUID> {
}
