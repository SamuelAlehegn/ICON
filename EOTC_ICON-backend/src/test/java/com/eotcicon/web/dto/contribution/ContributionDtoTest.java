package com.eotcicon.web.dto.contribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.domain.enums.ReviewAction;

class ContributionDtoTest {

	@Test
	void buildsAReviewDecisionResponse() {
		UUID submissionId = UUID.randomUUID();

		ReviewDecisionResponse response = ReviewDecisionResponse.of(submissionId, ReviewAction.APPROVE,
				PublicationStatus.APPROVED, "Published");

		assertThat(response.submissionId()).isEqualTo(submissionId);
		assertThat(response.action()).isEqualTo(ReviewAction.APPROVE);
		assertThat(response.status()).isEqualTo(PublicationStatus.APPROVED);
		assertThat(response.message()).isEqualTo("Published");
		assertThat(response.timestamp()).isNotNull();
	}

}
