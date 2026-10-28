package com.eotcicon.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.eotcicon.service.AuthService;
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

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register a new account")
	public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	@Operation(summary = "Login with email and password")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/refresh")
	@Operation(summary = "Refresh access token")
	public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return authService.refresh(request);
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
