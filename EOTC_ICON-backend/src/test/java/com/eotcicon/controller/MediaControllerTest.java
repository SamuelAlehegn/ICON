package com.eotcicon.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import com.eotcicon.domain.Icon;
import com.eotcicon.domain.IconImage;
import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.exception.ResourceNotFoundException;
import com.eotcicon.repository.IconImageRepository;
import com.eotcicon.service.CurrentUserService;

class MediaControllerTest {

	@TempDir
	Path mediaRoot;

	@Test
	void approvedMediaIsPublic() throws Exception {
		Fixture fixture = fixture(PublicationStatus.APPROVED);
		var response = fixture.controller().getMedia(fixture.iconId(), fixture.filename());
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
	}

	@Test
	void pendingMediaIsHiddenFromAnonymousCallers() throws Exception {
		Fixture fixture = fixture(PublicationStatus.PENDING);
		assertThrows(ResourceNotFoundException.class,
				() -> fixture.controller().getMedia(fixture.iconId(), fixture.filename()));
	}

	private Fixture fixture(PublicationStatus status) throws Exception {
		UUID iconId = UUID.randomUUID();
		String filename = "image.jpg";
		String key = "icons/" + iconId + "/" + filename;
		Path storedFile = mediaRoot.resolve(key);
		Files.createDirectories(storedFile.getParent());
		Files.write(storedFile, new byte[] { (byte) 0xff, (byte) 0xd8, (byte) 0xff });

		Icon icon = new Icon();
		icon.setPublicationStatus(status);
		IconImage image = new IconImage();
		image.setIcon(icon);
		image.setStorageKey(key);
		image.setContentType("image/jpeg");
		IconImageRepository imageRepository = mock(IconImageRepository.class);
		when(imageRepository.findMediaKey(iconId, key)).thenReturn(Optional.of(image));
		CurrentUserService currentUserService = mock(CurrentUserService.class);
		when(currentUserService.findCurrentUser()).thenReturn(Optional.empty());
		return new Fixture(new MediaController(imageRepository, currentUserService, mediaRoot.toString()), iconId, filename);
	}

	private record Fixture(MediaController controller, UUID iconId, String filename) {
	}
}
