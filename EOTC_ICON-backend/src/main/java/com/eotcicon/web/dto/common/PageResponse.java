package com.eotcicon.web.dto.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Envelope shared by every paginated endpoint.
 */
public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last) {

	public PageResponse {
		content = content == null ? List.of() : List.copyOf(content);
	}

	public static <T> PageResponse<T> of(Page<T> page) {
		return of(page, page.getContent());
	}

	public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
		return of(page, page.getContent().stream().map(mapper).toList());
	}

	public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
		return new PageResponse<>(
				content,
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast());
	}

}
