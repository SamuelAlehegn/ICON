package com.eotcicon.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

	private static final int MAX_REQUESTS = 5;
	private static final long WINDOW_SECONDS = 3600; // 1 hour

	private final Map<String, Window> windows = new ConcurrentHashMap<>();

	public boolean allowRequest(String key) {
		Instant now = Instant.now();
		Window window = windows.computeIfAbsent(key, k -> new Window(now));
		synchronized (window) {
			if (now.isAfter(window.windowStart.plusSeconds(WINDOW_SECONDS))) {
				window.windowStart = now;
				window.count = 0;
			}
			if (window.count >= MAX_REQUESTS) {
				return false;
			}
			window.count++;
			return true;
		}
	}

	private static class Window {
		private Instant windowStart;
		private int count;

		Window(Instant windowStart) {
			this.windowStart = windowStart;
			this.count = 0;
		}
	}
}
