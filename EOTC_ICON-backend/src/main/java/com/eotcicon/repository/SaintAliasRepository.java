package com.eotcicon.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eotcicon.domain.SaintAlias;

public interface SaintAliasRepository extends JpaRepository<SaintAlias, UUID> {
}
