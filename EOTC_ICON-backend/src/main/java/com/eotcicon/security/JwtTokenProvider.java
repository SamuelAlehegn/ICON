package com.eotcicon.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import jakarta.annotation.PostConstruct;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eotcicon.domain.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

	@Value("${app.jwt.secret:change-me-in-prod-change-me-in-prod-change-me}")
	private String jwtSecret;

	@Value("${app.jwt.access-token-expiration-ms:900000}")
	private long accessTokenExpirationMs;

	@PostConstruct
	void validateConfiguration() {
		if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
		}
		if (accessTokenExpirationMs <= 0) {
			throw new IllegalStateException("JWT access token expiration must be positive");
		}
	}

	public String generateAccessToken(User user) {
		Instant now = Instant.now();
		Instant expiry = now.plusMillis(accessTokenExpirationMs);
		return Jwts.builder()
				.subject(user.getId().toString())
				.claim("email", user.getEmail())
				.claim("role", user.getRole().name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiry))
				.signWith(getSigningKey())
				.compact();
	}

	public String getUserIdFromToken(String token) {
		Claims claims = parseClaims(token);
		return claims.getSubject();
	}

	public String getEmailFromToken(String token) {
		Claims claims = parseClaims(token);
		return claims.get("email", String.class);
	}

	public String getRoleFromToken(String token) {
		Claims claims = parseClaims(token);
		return claims.get("role", String.class);
	}

	public boolean validateToken(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(getSigningKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private SecretKey getSigningKey() {
		byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	public long getAccessTokenExpirationMs() {
		return accessTokenExpirationMs;
	}

}
