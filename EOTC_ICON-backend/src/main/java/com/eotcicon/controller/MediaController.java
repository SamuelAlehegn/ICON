package com.eotcicon.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.eotcicon.domain.Icon;
import com.eotcicon.domain.IconImage;
import com.eotcicon.domain.User;
import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.domain.enums.UserRole;
import com.eotcicon.exception.ResourceNotFoundException;
import com.eotcicon.repository.IconImageRepository;
import com.eotcicon.service.CurrentUserService;

@Controller
public class MediaController {

	private final IconImageRepository imageRepository;
	private final CurrentUserService currentUserService;
	private final Path mediaRoot;

	public MediaController(IconImageRepository imageRepository, CurrentUserService currentUserService,
			@Value("${app.storage.local-root:./data/media}") String localRoot) {
		this.imageRepository = imageRepository;
		this.currentUserService = currentUserService;
		this.mediaRoot = Paths.get(localRoot).toAbsolutePath().normalize();
	}

	@GetMapping("/media/icons/{iconId}/{filename}")
	@Transactional(readOnly = true)
	public ResponseEntity<Resource> getMedia(@PathVariable UUID iconId, @PathVariable String filename) {
		if (filename.isBlank() || filename.equals(".") || filename.equals("..")
				|| filename.contains("/") || filename.contains("\\")) {
			throw new ResourceNotFoundException("Media not found");
		}
		String key = "icons/" + iconId + "/" + filename;
		IconImage image = imageRepository.findMediaKey(iconId, key)
				.orElseThrow(() -> new ResourceNotFoundException("Media not found"));
		Icon icon = image.getIcon();
		boolean approved = icon.getPublicationStatus() == PublicationStatus.APPROVED;
		if (!approved && !mayViewUnpublished(icon)) {
			// Hide whether a non-public image exists from unauthenticated callers.
			throw new ResourceNotFoundException("Media not found");
		}

		Path file = resolveStoredFile(image.getStorageKey().equals(key)
				? image.getStorageKey() : image.getThumbnailStorageKey());
		MediaType mediaType = safeMediaType(image.getContentType());
		return ResponseEntity.ok()
				.header("X-Content-Type-Options", "nosniff")
				.cacheControl(approved ? CacheControl.maxAge(java.time.Duration.ofHours(1)).cachePublic()
						: CacheControl.noStore())
				.contentType(mediaType)
				.body(new FileSystemResource(file));
	}

	private boolean mayViewUnpublished(Icon icon) {
		User current = currentUserService.findCurrentUser().orElse(null);
		if (current == null) return false;
		if (current.getRole() == UserRole.ADMIN || current.getRole() == UserRole.MODERATOR) return true;
		if (icon.getContributor() != null && current.getId().equals(icon.getContributor().getId())) return true;
		return icon.getContactEmail() != null && icon.getContactEmail().equalsIgnoreCase(current.getEmail());
	}

	private Path resolveStoredFile(String storageKey) {
		try {
			Path realRoot = mediaRoot.toRealPath();
			Path candidate = realRoot.resolve(storageKey).normalize();
			if (!candidate.startsWith(realRoot)) throw new ResourceNotFoundException("Media not found");
			Path realFile = candidate.toRealPath();
			if (!realFile.startsWith(realRoot) || !Files.isRegularFile(realFile, LinkOption.NOFOLLOW_LINKS)) {
				throw new ResourceNotFoundException("Media not found");
			}
			return realFile;
		} catch (IOException ex) {
			throw new ResourceNotFoundException("Media not found");
		}
	}

	private MediaType safeMediaType(String contentType) {
		if (contentType == null) return MediaType.APPLICATION_OCTET_STREAM;
		return switch (contentType.toLowerCase(java.util.Locale.ROOT)) {
			case "image/jpeg", "image/jpg" -> MediaType.IMAGE_JPEG;
			case "image/png" -> MediaType.IMAGE_PNG;
			case "image/webp" -> MediaType.parseMediaType("image/webp");
			case "image/tiff" -> MediaType.parseMediaType("image/tiff");
			default -> MediaType.APPLICATION_OCTET_STREAM;
		};
	}
}
