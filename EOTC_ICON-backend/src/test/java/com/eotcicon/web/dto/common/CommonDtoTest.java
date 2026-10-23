package com.eotcicon.web.dto.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class CommonDtoTest {

	@Test
	void buildsThePageEnvelopeFromSpringData() {
		PageImpl<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 6);

		PageResponse<String> response = PageResponse.of(page);
		PageResponse<Integer> mapped = PageResponse.of(page, String::length);

		assertThat(response.content()).containsExactly("a", "b");
		assertThat(response.page()).isEqualTo(1);
		assertThat(response.size()).isEqualTo(2);
		assertThat(response.totalElements()).isEqualTo(6);
		assertThat(response.totalPages()).isEqualTo(3);
		assertThat(response.first()).isFalse();
		assertThat(response.last()).isFalse();
		assertThat(mapped.content()).containsExactly(1, 1);
	}

	@Test
	void buildsSuccessAndErrorResponses() {
		ActionResponse success = ActionResponse.success("Approved");
		ActionResponse failure = ActionResponse.failure("Rejected");
		ApiError error = ApiError.validation(400, "Bad Request", "Validation failed", "/api/v1/icons",
				List.of(new FieldErrorDto("title", "Title is required")));

		assertThat(success.success()).isTrue();
		assertThat(success.message()).isEqualTo("Approved");
		assertThat(success.timestamp()).isNotNull();
		assertThat(failure.success()).isFalse();
		assertThat(error.status()).isEqualTo(400);
		assertThat(error.error()).isEqualTo("Bad Request");
		assertThat(error.path()).isEqualTo("/api/v1/icons");
		assertThat(error.fieldErrors()).containsExactly(new FieldErrorDto("title", "Title is required"));
		assertThat(error.timestamp()).isNotNull();
	}

}
