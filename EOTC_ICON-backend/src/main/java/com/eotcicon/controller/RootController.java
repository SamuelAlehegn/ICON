package com.eotcicon.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

	@GetMapping("/")
	Map<String, Object> root() {
		return Map.of(
			"application", "EOTC_ICON-backend",
			"status", "running",
			"timestamp", Instant.now().toString(),
			"h2Console", "/h2-console"
		);
	}

}
