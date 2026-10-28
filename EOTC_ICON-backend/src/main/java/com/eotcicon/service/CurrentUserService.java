package com.eotcicon.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.eotcicon.domain.User;
import com.eotcicon.repository.UserRepository;

@Service
public class CurrentUserService {

	private final UserRepository userRepository;

	public CurrentUserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User requireCurrentUser() {
		return findCurrentUser()
				.orElseThrow(() -> new AccessDeniedException("An authenticated account is required"));
	}

	public Optional<User> findCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return Optional.empty();
		}

		String name = authentication.getName();
		if (!StringUtils.hasText(name)) {
			return Optional.empty();
		}

		Optional<User> byEmail = userRepository.findByEmail(name.toLowerCase());
		if (byEmail.isPresent()) {
			return byEmail;
		}

		try {
			return userRepository.findById(UUID.fromString(name));
		} catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

}
