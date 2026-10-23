package com.eotcicon.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eotcicon.domain.ChurchAlias;

public interface ChurchAliasRepository extends JpaRepository<ChurchAlias, UUID> {
}
