package com.eotcicon.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Root", description = "Health/info endpoints")
public class RootController {

	@GetMapping("/")
	@Operation(summary = "Health check")
	Map<String, Object> root() {
		return Map.of(
			"application", "EOTC_ICON-backend",
			"status", "running",
			"timestamp", Instant.now().toString(),
			"h2Console", "/h2-console"
		);
	}

}
