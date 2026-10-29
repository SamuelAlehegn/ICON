package com.eotcicon.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;

import com.eotcicon.service.AuthService;
import com.eotcicon.service.RateLimitService;
import com.eotcicon.web.dto.auth.LoginRequest;
import com.eotcicon.web.dto.auth.RefreshTokenRequest;
import com.eotcicon.web.dto.auth.RegisterRequest;
import com.eotcicon.web.dto.auth.TokenResponse;
import com.eotcicon.web.dto.auth.UserProfileDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, login, refresh and profile")
public class AuthController {

	private final AuthService authService;
	private final RateLimitService rateLimitService;

	public AuthController(AuthService authService, RateLimitService rateLimitService) {
		this.authService = authService;
		this.rateLimitService = rateLimitService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register a new account")
	public TokenResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
		limit("register", httpRequest.getRemoteAddr(), request.email(), 5, 3600);
		return authService.register(request);
	}

	@PostMapping("/login")
	@Operation(summary = "Login with email and password")
	public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		limit("login", httpRequest.getRemoteAddr(), request.email(), 10, 900);
		return authService.login(request);
	}

	@PostMapping("/refresh")
	@Operation(summary = "Refresh access token")
	public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
		limit("refresh", httpRequest.getRemoteAddr(), "", 30, 900);
		return authService.refresh(request);
	}

	private void limit(String action, String address, String email, int max, long seconds) {
		String ip = address == null ? "unknown" : address;
		String normalizedEmail = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
		boolean allowed = rateLimitService.allowRequest("auth:" + action + ":ip:" + ip, max, seconds);
		if (!normalizedEmail.isBlank()) {
			allowed = rateLimitService.allowRequest("auth:" + action + ":email:" + normalizedEmail, max, seconds) && allowed;
		}
		if (!allowed) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later.");
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Logout and revoke refresh token")
	public void logout(@Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(request);
	}

	@GetMapping("/me")
	@Operation(summary = "Get current authenticated user profile")
	public UserProfileDto me(Authentication authentication) {
		String userId = (String) authentication.getPrincipal();
		return authService.getCurrentUser(userId);
	}

}
