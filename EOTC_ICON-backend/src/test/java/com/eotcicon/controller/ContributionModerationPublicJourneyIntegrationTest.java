package com.eotcicon.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.eotcicon.domain.Icon;
import com.eotcicon.domain.User;
import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.domain.enums.ReviewAction;
import com.eotcicon.domain.enums.UserRole;
import com.eotcicon.repository.ContributionReviewRepository;
import com.eotcicon.repository.IconRepository;
import com.eotcicon.repository.UserRepository;
import com.eotcicon.service.storage.StorageService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class ContributionModerationPublicJourneyIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private IconRepository iconRepository;

	@Autowired
	private ContributionReviewRepository reviewRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@MockitoBean
	private StorageService storageService;

	private User moderator;
	private final String suffix = UUID.randomUUID().toString().substring(0, 8);
	private final String title = "Journey icon " + suffix;

	@BeforeEach
	void setUp() throws Exception {
		moderator = new User();
		moderator.setEmail("moderator-" + suffix + "@example.org");
		moderator.setPasswordHash("unused-test-hash");
		moderator.setDisplayName("Archive Moderator");
		moderator.setRole(UserRole.MODERATOR);
		moderator = userRepository.save(moderator);
		when(storageService.store(any(), anyString())).thenAnswer(invocation -> {
			org.springframework.web.multipart.MultipartFile file = invocation.getArgument(0);
			String key = invocation.getArgument(1);
			return new StorageService.StoredFile(key, file.getSize(), file.getContentType());
		});
	}

	@Test
	void searchOpenContributeModerateAndPublishJourney() throws Exception {
		Icon publicReference = persistIcon("public-" + suffix, PublicationStatus.APPROVED);
		publicReference.setTitle("Public archive icon " + suffix);
		iconRepository.save(publicReference);
		mockMvc.perform(get("/api/v1/icons").param("q", publicReference.getTitle()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1));
		mockMvc.perform(get("/api/v1/icons/{slug}", publicReference.getSlug()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value(publicReference.getTitle()));

		mockMvc.perform(get("/api/v1/icons").param("q", title))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty());
		mockMvc.perform(get("/api/v1/icons/{slug}", slug()))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/statistics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.publishedIcons").value(1));

		String referenceCode = submitContribution();
		Icon submitted = iconRepository.findByReferenceCode(referenceCode).orElseThrow();
		mockMvc.perform(get("/api/v1/contributions/status/{referenceCode}", referenceCode))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDING"));

		mockMvc.perform(get("/api/v1/admin/submissions")
					.param("status", "PENDING")
					.param("q", title)
					.with(user(moderator.getEmail()).roles("MODERATOR")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].id").value(submitted.getId().toString()));

		mockMvc.perform(get("/api/v1/admin/submissions/{id}", submitted.getId())
					.with(user(moderator.getEmail()).roles("MODERATOR")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value(title));

		mockMvc.perform(post("/api/v1/admin/submissions/{id}/decision", submitted.getId())
					.with(user(moderator.getEmail()).roles("MODERATOR"))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"action\":\"APPROVE\",\"notes\":\"Rights and attribution verified\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.action").value("APPROVE"))
				.andExpect(jsonPath("$.status").value("APPROVED"));

		mockMvc.perform(get("/api/v1/icons").param("q", title))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].slug").value(slug()));
		mockMvc.perform(get("/api/v1/icons/{slug}", slug()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value(title));
		mockMvc.perform(get("/api/v1/statistics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.publishedIcons").value(2));
		org.junit.jupiter.api.Assertions.assertTrue(reviewRepository.findAll().stream()
				.anyMatch(review -> review.getIcon().getId().equals(submitted.getId())
						&& review.getAction() == ReviewAction.APPROVE));
	}

	@Test
	void unapprovedAndRejectedRecordsNeverAppearInPublicSearchOrStats() throws Exception {
		Icon pending = persistIcon("pending-" + suffix, PublicationStatus.PENDING);
		Icon rejected = persistIcon("rejected-" + suffix, PublicationStatus.REJECTED);

		mockMvc.perform(get("/api/v1/icons").param("q", suffix))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty());
		mockMvc.perform(get("/api/v1/icons/{slug}", pending.getSlug())).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/icons/{slug}", rejected.getSlug())).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/statistics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.publishedIcons").value(0));
	}

	@Test
	void contributorsCannotReachModerationDecisions() throws Exception {
		Icon pending = persistIcon("restricted-" + suffix, PublicationStatus.PENDING);
		mockMvc.perform(post("/api/v1/admin/submissions/{id}/decision", pending.getId())
					.with(user("contributor-" + suffix).roles("CONTRIBUTOR"))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"action\":\"APPROVE\"}"))
				.andExpect(status().isForbidden());
	}

	private String submitContribution() throws Exception {
		MockMultipartFile image = new MockMultipartFile("images", "icon.jpg", "image/jpeg",
				new byte[] { (byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00 });
		MvcResult result = mockMvc.perform(multipart("/api/v1/contributions")
				.file(image)
				.param("churchName", "Journey Church " + suffix)
				.param("title", title)
				.param("contributorDisplayName", "Journey Contributor")
				.param("contactEmail", "journey-" + suffix + "@example.org")
				.param("consent", "true"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("referenceCode").asText();
	}

	private Icon persistIcon(String slug, PublicationStatus status) {
		Icon icon = new Icon();
		icon.setReferenceCode("EOTC-" + slug.toUpperCase().replace('-', 'X'));
		icon.setSlug(slug);
		icon.setTitle("Hidden " + slug);
		icon.setPublicationStatus(status);
		if (status == PublicationStatus.APPROVED) icon.setPublishedAt(java.time.Instant.now());
		return iconRepository.save(icon);
	}

	private String slug() {
		return title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
	}
}
