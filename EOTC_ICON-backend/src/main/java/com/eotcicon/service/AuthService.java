package com.eotcicon.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eotcicon.domain.RefreshToken;
import com.eotcicon.domain.User;
import com.eotcicon.domain.enums.UserRole;
import com.eotcicon.domain.enums.UserStatus;
import com.eotcicon.exception.ResourceNotFoundException;
import com.eotcicon.repository.RefreshTokenRepository;
import com.eotcicon.repository.UserRepository;
import com.eotcicon.security.JwtTokenProvider;
import com.eotcicon.web.dto.auth.LoginRequest;
import com.eotcicon.web.dto.auth.RefreshTokenRequest;
import com.eotcicon.web.dto.auth.RegisterRequest;
import com.eotcicon.web.dto.auth.TokenResponse;
import com.eotcicon.web.dto.auth.UserProfileDto;
import com.eotcicon.web.mapper.UserMapper;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtTokenProvider tokenProvider;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
			JwtTokenProvider tokenProvider, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.tokenProvider = tokenProvider;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public TokenResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.email().toLowerCase())) {
			throw new IllegalArgumentException("Email already registered");
		}
		User user = new User();
		user.setEmail(request.email().toLowerCase());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setDisplayName(request.displayName());
		user.setRole(UserRole.CONTRIBUTOR);
		user.setStatus(UserStatus.ACTIVE);
		user = userRepository.save(user);
		user.setLastLoginAt(Instant.now());
		return issueTokens(user);
	}

	@Transactional
	public TokenResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email().toLowerCase())
				.orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Invalid credentials"));
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
		}
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new org.springframework.security.authentication.BadCredentialsException("Account is not active");
		}
		user.setLastLoginAt(Instant.now());
		return issueTokens(user);
	}

	@Transactional
	public TokenResponse refresh(RefreshTokenRequest request) {
		String rawToken = request.refreshToken();
		String tokenHash = hashToken(rawToken);
		RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
				.orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Invalid refresh token"));
		if (stored.getRevokedAt() != null) {
			throw new org.springframework.security.authentication.BadCredentialsException("Refresh token revoked");
		}
		if (stored.getExpiresAt().isBefore(Instant.now())) {
			throw new org.springframework.security.authentication.BadCredentialsException("Refresh token expired");
		}
		User user = stored.getUser();
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new org.springframework.security.authentication.BadCredentialsException("Account is not active");
		}
		// rotate
		String newRawToken = UUID.randomUUID().toString() + "." + UUID.randomUUID().toString();
		RefreshToken newToken = new RefreshToken();
		newToken.setUser(user);
		newToken.setTokenHash(hashToken(newRawToken));
		newToken.setIssuedAt(Instant.now());
		newToken.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
		newToken = refreshTokenRepository.save(newToken);
		stored.setRevokedAt(Instant.now());
		stored.setReplacedByToken(newToken);
		refreshTokenRepository.save(stored);
		String accessToken = tokenProvider.generateAccessToken(user);
		return new TokenResponse(accessToken, newRawToken, "Bearer", tokenProvider.getAccessTokenExpirationMs() / 1000);
	}

	@Transactional
	public void logout(RefreshTokenRequest request) {
		String rawToken = request.refreshToken();
		String tokenHash = hashToken(rawToken);
		refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(stored -> {
			if (stored.getRevokedAt() == null) {
				stored.setRevokedAt(Instant.now());
				refreshTokenRepository.save(stored);
			}
		});
	}

	@Transactional(readOnly = true)
	public UserProfileDto getCurrentUser(String userId) {
		UUID id = UUID.fromString(userId);
		User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", userId));
		return UserMapper.toProfile(user);
	}

	private TokenResponse issueTokens(User user) {
		String accessToken = tokenProvider.generateAccessToken(user);
		String refreshRaw = UUID.randomUUID().toString() + "." + UUID.randomUUID().toString();
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setUser(user);
		refreshToken.setTokenHash(hashToken(refreshRaw));
		refreshToken.setIssuedAt(Instant.now());
		refreshToken.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
		refreshTokenRepository.save(refreshToken);
		return new TokenResponse(accessToken, refreshRaw, "Bearer", tokenProvider.getAccessTokenExpirationMs() / 1000);
	}

	private String hashToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

}
