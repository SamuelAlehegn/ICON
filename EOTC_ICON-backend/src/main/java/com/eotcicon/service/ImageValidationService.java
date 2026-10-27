package com.eotcicon.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageValidationService {

	private static final long MAX_FILE_SIZE = 15L * 1024L * 1024L;
	private static final Map<String, byte[]> MAGIC_BYTES = new HashMap<>();

	static {
		MAGIC_BYTES.put("image/jpeg", new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });
		MAGIC_BYTES.put("image/png", new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A });
		MAGIC_BYTES.put("image/webp", "RIFF".getBytes());
		MAGIC_BYTES.put("image/tiff", new byte[] { 0x49, 0x49, 0x2A, 0x00 });
		MAGIC_BYTES.put("image/tiff-big-endian", new byte[] { 0x4D, 0x4D, 0x00, 0x2A });
	}

	public void validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Image file is required");
		}
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new IllegalArgumentException("Each image must be 15 MB or smaller");
		}
		String contentType = file.getContentType();
		if (contentType == null || !isAllowedType(contentType)) {
			throw new IllegalArgumentException("Only JPEG, PNG, WebP, and TIFF images are allowed");
		}
		try {
			validateMagicBytes(file);
		} catch (IOException e) {
			throw new IllegalArgumentException("Invalid or unreadable image file");
		}
	}

	private boolean isAllowedType(String contentType) {
		String type = contentType.toLowerCase();
		return type.equals("image/jpeg") || type.equals("image/jpg") || type.equals("image/png")
				|| type.equals("image/webp") || type.equals("image/tiff");
	}

	private void validateMagicBytes(MultipartFile file) throws IOException {
		String contentType = file.getContentType().toLowerCase();
		byte[] bytes = new byte[12];
		try (InputStream in = file.getInputStream()) {
			int read = in.read(bytes);
			if (read < 3) {
				throw new IllegalArgumentException("Invalid image file");
			}
		}
		if (contentType.equals("image/jpeg") || contentType.equals("image/jpg")) {
			if (!(bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF)) {
				throw new IllegalArgumentException("Invalid JPEG image file");
			}
			return;
		}
		if (contentType.equals("image/png")) {
			if (!(bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
					&& bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A)) {
				throw new IllegalArgumentException("Invalid PNG image file");
			}
			return;
		}
		if (contentType.equals("image/webp")) {
			if (!(bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46)) {
				throw new IllegalArgumentException("Invalid WebP image file");
			}
			return;
		}
		if (contentType.equals("image/tiff")) {
			if (!((bytes[0] == 0x49 && bytes[1] == 0x49 && bytes[2] == 0x2A && bytes[3] == 0x00)
					|| (bytes[0] == 0x4D && bytes[1] == 0x4D && bytes[2] == 0x00 && bytes[3] == 0x2A))) {
				throw new IllegalArgumentException("Invalid TIFF image file");
			}
		}
	}
}
