package com.eotcicon.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.eotcicon.domain.ContributionReview;
import com.eotcicon.domain.Icon;
import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.exception.ResourceNotFoundException;
import com.eotcicon.repository.ContributionReviewRepository;
import com.eotcicon.repository.IconRepository;
import com.eotcicon.web.dto.contribution.AdminSubmissionDetailDto;
import com.eotcicon.web.dto.contribution.ReviewDecisionRequest;
import com.eotcicon.web.dto.contribution.ReviewDecisionResponse;
import com.eotcicon.web.dto.contribution.SubmissionSummaryDto;
import com.eotcicon.web.mapper.ContributionMapper;

import java.util.function.Function;

@Service
@Transactional
public class AdminSubmissionService {

	private final IconRepository iconRepository;
	private final ContributionReviewRepository reviewRepository;
	private final CurrentUserService currentUserService;
	private final Function<String, String> mediaUrlResolver;

	public AdminSubmissionService(IconRepository iconRepository, ContributionReviewRepository reviewRepository,
			CurrentUserService currentUserService, Function<String, String> mediaUrlResolver) {
		this.iconRepository = iconRepository;
		this.reviewRepository = reviewRepository;
		this.currentUserService = currentUserService;
		this.mediaUrlResolver = mediaUrlResolver;
	}

	@Transactional(readOnly = true)
	public Page<SubmissionSummaryDto> getQueue(PublicationStatus status, String query, int page, int size) {
		if (page < 0) throw new IllegalArgumentException("Page must be zero or greater");
		if (size < 1 || size > 100) throw new IllegalArgumentException("Page size must be between 1 and 100");
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		String normalizedQuery = StringUtils.hasText(query) ? query.trim() : null;
		return iconRepository.searchModerationQueue(status, normalizedQuery, pageable)
				.map(ContributionMapper::toSummary);
	}

	@Transactional(readOnly = true)
	public AdminSubmissionDetailDto getSubmission(UUID id) {
		return ContributionMapper.toAdminDetail(findIcon(id), mediaUrlResolver);
	}

	public ReviewDecisionResponse decide(UUID id, ReviewDecisionRequest request) {
		Icon icon = findIcon(id);
		PublicationStatus from = icon.getPublicationStatus();
		if (from != PublicationStatus.PENDING && from != PublicationStatus.NEEDS_CORRECTION) {
			throw new IllegalStateException("Only pending submissions or submissions needing correction can be reviewed");
		}
		PublicationStatus to = switch (request.action()) {
			case APPROVE -> PublicationStatus.APPROVED;
			case REJECT -> PublicationStatus.REJECTED;
			case REQUEST_CORRECTION -> PublicationStatus.NEEDS_CORRECTION;
			default -> throw new IllegalArgumentException("Unsupported moderation action");
		};
		icon.setPublicationStatus(to);
		String notes = StringUtils.hasText(request.notes()) ? request.notes().trim() : null;
		icon.setModerationNotes(notes);
		icon.setPublishedAt(to == PublicationStatus.APPROVED ? Instant.now() : null);
		iconRepository.save(icon);

		ContributionReview review = new ContributionReview();
		review.setIcon(icon);
		review.setReviewer(currentUserService.requireCurrentUser());
		review.setAction(request.action());
		review.setFromStatus(from);
		review.setToStatus(to);
		review.setNotes(notes);
		reviewRepository.save(review);

		String message = switch (request.action()) {
			case APPROVE -> "Submission approved and published.";
			case REJECT -> "Submission rejected.";
			case REQUEST_CORRECTION -> "Correction requested.";
			default -> "Moderation decision recorded.";
		};
		return ReviewDecisionResponse.of(icon.getId(), request.action(), to, message);
	}

	private Icon findIcon(UUID id) {
		return iconRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
	}
}
