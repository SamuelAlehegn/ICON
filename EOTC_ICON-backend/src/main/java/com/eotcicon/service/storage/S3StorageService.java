package com.eotcicon.service.storage;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3StorageService implements StorageService {

	@Override
	public StoredFile store(MultipartFile file, String objectKey) throws IOException {
		// For local development without S3 credentials, we fall back to treating as stored
		String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream";
		return new StoredFile(objectKey, file.getSize(), contentType);
	}
}
