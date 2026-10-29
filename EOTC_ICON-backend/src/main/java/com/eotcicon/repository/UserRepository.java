package com.eotcicon.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eotcicon.domain.User;
import com.eotcicon.domain.enums.UserRole;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	long countByRole(UserRole role);
}
