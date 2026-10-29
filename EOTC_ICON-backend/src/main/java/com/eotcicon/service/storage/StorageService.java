package com.eotcicon.service.storage;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

	static void validateObjectKey(String objectKey) {
		if (objectKey == null || objectKey.isBlank() || objectKey.startsWith("/")
				|| objectKey.indexOf('\\') >= 0 || objectKey.chars().anyMatch(Character::isISOControl)) {
			throw new IllegalArgumentException("Invalid storage key");
		}
		for (String segment : objectKey.split("/", -1)) {
			if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
				throw new IllegalArgumentException("Invalid storage key");
			}
		}
	}

	StoredFile store(MultipartFile file, String objectKey) throws IOException;

	record StoredFile(String key, long size, String contentType) {
	}
}
