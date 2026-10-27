package com.eotcicon.service.storage;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

	StoredFile store(MultipartFile file, String objectKey) throws IOException;

	record StoredFile(String key, long size, String contentType) {
	}
}
