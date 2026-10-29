package com.eotcicon.service;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageValidationService {

	private static final long MAX_FILE_SIZE = 15L * 1024L * 1024L;
	private static final int MAX_IMAGE_COUNT = 4;
	private static final long MAX_TOTAL_SIZE = 60L * 1024L * 1024L;

	public void validateBatch(java.util.List<MultipartFile> files) {
		if (files == null || files.isEmpty() || files.size() > MAX_IMAGE_COUNT) {
			throw new IllegalArgumentException("Submit between one and four images");
		}
		long total = 0;
		for (MultipartFile file : files) {
			validate(file);
			total += file.getSize();
			if (total > MAX_TOTAL_SIZE) throw new IllegalArgumentException("Combined image size must be 60 MB or smaller");
		}
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
		byte[] bytes;
		try (InputStream in = file.getInputStream()) {
			bytes = in.readNBytes(12);
			if (bytes.length < 4) {
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
			if (!(bytes.length >= 12 && bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46
					&& bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50)) {
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
