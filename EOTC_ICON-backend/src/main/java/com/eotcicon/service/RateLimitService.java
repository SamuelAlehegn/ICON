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
		return allowRequest(key, MAX_REQUESTS, WINDOW_SECONDS);
	}

	public boolean allowRequest(String key, int maxRequests, long windowSeconds) {
		if (key == null || key.isBlank() || maxRequests < 1 || windowSeconds < 1) {
			return false;
		}
		Instant now = Instant.now();
		// Opportunistically discard expired buckets so arbitrary client keys cannot
		// grow this in-memory limiter without bound.
		if (windows.size() > 10_000) {
			windows.entrySet().removeIf(entry -> now.isAfter(entry.getValue().windowStart.plusSeconds(windowSeconds * 2)));
		}
		Window window = windows.computeIfAbsent(key, k -> new Window(now));
		synchronized (window) {
			if (!now.isBefore(window.windowStart.plusSeconds(windowSeconds))) {
				window.windowStart = now;
				window.count = 0;
			}
			if (window.count >= maxRequests) {
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
