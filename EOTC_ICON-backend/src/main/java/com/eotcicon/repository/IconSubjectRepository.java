package com.eotcicon.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eotcicon.domain.IconSubject;

public interface IconSubjectRepository extends JpaRepository<IconSubject, UUID> {
}
