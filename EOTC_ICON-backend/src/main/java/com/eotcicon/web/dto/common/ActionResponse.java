package com.eotcicon.web.dto.common;

import java.time.Instant;

/**
 * Result of an action that has no resource representation of its own, such as a
 * logout or a moderation decision.
 */
public record ActionResponse(boolean success, String message, Instant timestamp) {

	public static ActionResponse success(String message) {
		return new ActionResponse(true, message, Instant.now());
	}

	public static ActionResponse failure(String message) {
		return new ActionResponse(false, message, Instant.now());
	}

}
