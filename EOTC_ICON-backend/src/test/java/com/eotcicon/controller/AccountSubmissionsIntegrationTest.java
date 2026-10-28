package com.eotcicon.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.eotcicon.domain.ContributionReview;
import com.eotcicon.domain.Icon;
import com.eotcicon.domain.User;
import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.domain.enums.ReviewAction;
import com.eotcicon.repository.ContributionReviewRepository;
import com.eotcicon.repository.IconRepository;
import com.eotcicon.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AccountSubmissionsIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private IconRepository iconRepository;

	@Autowired
	private ContributionReviewRepository contributionReviewRepository;

	private User owner;
	private Icon icon;

	@BeforeEach
	void setUp() {
		owner = new User();
		owner.setEmail("owner@example.org");
		owner.setPasswordHash("unused");
		owner.setDisplayName("Archive Owner");
		owner = userRepository.save(owner);

		icon = new Icon();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		icon.setReferenceCode("EOTC-" + suffix.toUpperCase());
		icon.setSlug("account-test-" + suffix);
		icon.setTitle("Original title");
		icon.setDescription("Original description");
		icon.setContributor(owner);
		icon.setContactEmail(owner.getEmail());
		icon.setPublicationStatus(PublicationStatus.PENDING);
		icon = iconRepository.save(icon);
	}

	@Test
	void listReturnsOnlyTheSignedInContributorsSubmissions() throws Exception {
		User other = new User();
		other.setEmail("other@example.org");
		other.setPasswordHash("unused");
		other.setDisplayName("Other Contributor");
		other = userRepository.save(other);

		Icon otherIcon = new Icon();
		otherIcon.setReferenceCode("EOTC-OTHER");
		otherIcon.setSlug("other-submission");
		otherIcon.setTitle("Someone else's icon");
		otherIcon.setContributor(other);
		otherIcon.setPublicationStatus(PublicationStatus.PENDING);
		iconRepository.save(otherIcon);

		mockMvc.perform(get("/api/v1/account/submissions").with(user(owner.getEmail())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].referenceCode").value(icon.getReferenceCode()));
	}

	@Test
	void contributorCanUpdatePermittedFields() throws Exception {
		String body = """
				{
				  "title": "Corrected title",
				  "description": "Corrected description",
				  "category": "Icon"
				}
				""";

		mockMvc.perform(patch("/api/v1/account/submissions/{id}", icon.getId())
						.with(user(owner.getEmail()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Corrected title"))
				.andExpect(jsonPath("$.description").value("Corrected description"))
				.andExpect(jsonPath("$.category").value("Icon"));
	}

	@Test
	void anotherContributorCannotUpdateTheSubmission() throws Exception {
		User other = new User();
		other.setEmail("other@example.org");
		other.setPasswordHash("unused");
		other.setDisplayName("Other Contributor");
		other = userRepository.save(other);

		mockMvc.perform(patch("/api/v1/account/submissions/{id}", icon.getId())
						.with(user(other.getEmail()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Hijacked\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void correctionRequestMarksTheSubmissionAndAppendsReview() throws Exception {
		mockMvc.perform(post("/api/v1/account/submissions/{id}/request-correction", icon.getId())
						.with(user(owner.getEmail()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"notes\":\"The date is wrong\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.action").value("REQUEST_CORRECTION"))
				.andExpect(jsonPath("$.status").value("NEEDS_CORRECTION"));

		ContributionReview review = contributionReviewRepository.findAll().get(0);
		expectReview(review, ReviewAction.REQUEST_CORRECTION, PublicationStatus.NEEDS_CORRECTION);
	}

	@Test
	void removalRequestArchivesTheSubmissionAndAppendsReview() throws Exception {
		mockMvc.perform(post("/api/v1/account/submissions/{id}/request-removal", icon.getId())
						.with(user(owner.getEmail()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"notes\":\"Please remove this record\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.action").value("WITHDRAW"))
				.andExpect(jsonPath("$.status").value("ARCHIVED"));

		ContributionReview review = contributionReviewRepository.findAll().get(0);
		expectReview(review, ReviewAction.WITHDRAW, PublicationStatus.ARCHIVED);
	}

	private void expectReview(ContributionReview review, ReviewAction action, PublicationStatus toStatus) {
		org.junit.jupiter.api.Assertions.assertEquals(action, review.getAction());
		org.junit.jupiter.api.Assertions.assertEquals(PublicationStatus.PENDING, review.getFromStatus());
		org.junit.jupiter.api.Assertions.assertEquals(toStatus, review.getToStatus());
	}

}
