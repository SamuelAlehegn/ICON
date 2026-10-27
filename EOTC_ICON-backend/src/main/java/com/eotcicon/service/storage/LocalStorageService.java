package com.eotcicon.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

	private final Path root;

	public LocalStorageService(@Value("${app.storage.local-root:./data/media}") String localRoot) {
		this.root = Paths.get(localRoot).toAbsolutePath().normalize();
	}

	@Override
	public StoredFile store(MultipartFile file, String objectKey) throws IOException {
		if (objectKey == null || objectKey.isBlank()) {
			throw new IllegalArgumentException("objectKey must not be blank");
		}
		Path target = root.resolve(objectKey).normalize();
		if (!target.startsWith(root)) {
			throw new IllegalArgumentException("Invalid storage key");
		}
		Files.createDirectories(target.getParent());
		try (InputStream in = file.getInputStream()) {
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		}
		String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream";
		return new StoredFile(objectKey, file.getSize(), contentType);
	}
}
