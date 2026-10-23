package com.eotcicon.web.mapper;

import com.eotcicon.domain.User;
import com.eotcicon.web.dto.auth.UserProfileDto;

/**
 * Maps an account entity to the profile returned by the authentication API.
 */
public final class UserMapper {

	private UserMapper() {
	}

	public static UserProfileDto toProfile(User user) {
		if (user == null) {
			return null;
		}
		return new UserProfileDto(
				user.getId(),
				user.getEmail(),
				user.getDisplayName(),
				user.getRole(),
				user.getStatus(),
				user.getLastLoginAt(),
				user.getCreatedAt());
	}

}
