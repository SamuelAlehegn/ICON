package com.eotcicon.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.eotcicon.domain.ContributionReview;
import com.eotcicon.domain.Icon;
import com.eotcicon.domain.User;
import com.eotcicon.domain.enums.AttributionPreference;
import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.domain.enums.ReviewAction;
import com.eotcicon.exception.ResourceNotFoundException;
import com.eotcicon.repository.ContributionReviewRepository;
import com.eotcicon.repository.IconRepository;
import com.eotcicon.web.dto.contribution.AccountSubmissionDto;
import com.eotcicon.web.dto.contribution.ReviewDecisionResponse;
import com.eotcicon.web.dto.contribution.SubmissionRequestDto;
import com.eotcicon.web.dto.contribution.UpdateSubmissionRequest;
import com.eotcicon.web.mapper.ContributionMapper;

@Service
@Transactional
public class AccountService {

	private final IconRepository iconRepository;
	private final ContributionReviewRepository contributionReviewRepository;
	private final CurrentUserService currentUserService;
	private final Function<String, String> mediaUrlResolver;

	public AccountService(IconRepository iconRepository, ContributionReviewRepository contributionReviewRepository,
			CurrentUserService currentUserService, Function<String, String> mediaUrlResolver) {
		this.iconRepository = iconRepository;
		this.contributionReviewRepository = contributionReviewRepository;
		this.currentUserService = currentUserService;
		this.mediaUrlResolver = mediaUrlResolver;
	}

	@Transactional(readOnly = true)
	public List<AccountSubmissionDto> getMySubmissions() {
		User user = currentUserService.requireCurrentUser();
		return iconRepository.findOwnedBy(user.getId(), user.getEmail()).stream()
				.map(icon -> ContributionMapper.toAccountDetail(icon, mediaUrlResolver))
				.toList();
	}

	@Transactional(readOnly = true)
	public AccountSubmissionDto getMySubmission(UUID submissionId) {
		User user = currentUserService.requireCurrentUser();
		return ContributionMapper.toAccountDetail(findOwned(submissionId, user), mediaUrlResolver);
	}

	public AccountSubmissionDto updateSubmission(UUID submissionId, UpdateSubmissionRequest request) {
		User user = currentUserService.requireCurrentUser();
		Icon icon = findOwned(submissionId, user);
		requireEditable(icon);

		PublicationStatus status = icon.getPublicationStatus();
		boolean changed = applyChanges(icon, request);
		iconRepository.save(icon);

		if (changed) {
			appendReview(icon, user, ReviewAction.COMMENT, status, status,
					"Contributor updated submission details.");
			iconRepository.save(icon);
		}

		return ContributionMapper.toAccountDetail(icon, mediaUrlResolver);
	}

	public ReviewDecisionResponse requestCorrection(UUID submissionId, SubmissionRequestDto request) {
		User user = currentUserService.requireCurrentUser();
		Icon icon = findOwned(submissionId, user);
		PublicationStatus from = icon.getPublicationStatus();
		if (from == PublicationStatus.ARCHIVED) {
			throw new IllegalStateException("Archived submissions cannot be changed");
		}

		PublicationStatus to = PublicationStatus.NEEDS_CORRECTION;
		icon.setPublicationStatus(to);
		iconRepository.save(icon);
		appendReview(icon, user, ReviewAction.REQUEST_CORRECTION, from, to, notesOf(request));
		return ReviewDecisionResponse.of(icon.getId(), ReviewAction.REQUEST_CORRECTION, to,
				"Your correction request has been sent to the archive team.");
	}

	public ReviewDecisionResponse requestRemoval(UUID submissionId, SubmissionRequestDto request) {
		User user = currentUserService.requireCurrentUser();
		Icon icon = findOwned(submissionId, user);
		PublicationStatus from = icon.getPublicationStatus();
		if (from == PublicationStatus.ARCHIVED) {
			return ReviewDecisionResponse.of(icon.getId(), ReviewAction.WITHDRAW, from,
					"This submission has already been archived.");
		}

		PublicationStatus to = PublicationStatus.ARCHIVED;
		icon.setPublicationStatus(to);
		iconRepository.save(icon);
		appendReview(icon, user, ReviewAction.WITHDRAW, from, to, notesOf(request));
		return ReviewDecisionResponse.of(icon.getId(), ReviewAction.WITHDRAW, to,
				"Your removal request has been received.");
	}

	private Icon findOwned(UUID submissionId, User user) {
		Icon icon = iconRepository.findById(submissionId)
				.orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
		boolean ownedByUser = icon.getContributor() != null
				&& Objects.equals(icon.getContributor().getId(), user.getId());
		boolean sameEmail = StringUtils.hasText(icon.getContactEmail())
				&& icon.getContactEmail().equalsIgnoreCase(user.getEmail());
		if (!ownedByUser && !sameEmail) {
			throw new ResourceNotFoundException("Submission not found");
		}
		return icon;
	}

	private void requireEditable(Icon icon) {
		PublicationStatus status = icon.getPublicationStatus();
		if (status != PublicationStatus.PENDING && status != PublicationStatus.NEEDS_CORRECTION) {
			throw new IllegalStateException("This submission can no longer be edited directly. "
					+ "Please request a correction instead.");
		}
	}

	private boolean applyChanges(Icon icon, UpdateSubmissionRequest request) {
		boolean changed = false;

		if (request.title() != null) {
			String title = blankToNull(request.title());
			if (title == null) {
				throw new IllegalArgumentException("Title cannot be blank");
			}
			icon.setTitle(title);
			changed = true;
		}
		if (request.description() != null) {
			icon.setDescription(blankToNull(request.description()));
			changed = true;
		}
		if (request.historicalNotes() != null) {
			icon.setHistoricalNotes(blankToNull(request.historicalNotes()));
			changed = true;
		}
		if (request.category() != null) {
			icon.setCategory(blankToNull(request.category()));
			changed = true;
		}
		if (request.captureDate() != null) {
			icon.setCaptureDate(request.captureDate());
			changed = true;
		}
		if (request.artworkDateStart() != null) {
			icon.setArtworkDateStart(request.artworkDateStart());
			changed = true;
		}
		if (request.artworkDateEnd() != null) {
			icon.setArtworkDateEnd(request.artworkDateEnd());
			changed = true;
		}
		if (request.attributionPreference() != null) {
			icon.setAttributionPreference(request.attributionPreference());
			if (request.attributionPreference() == AttributionPreference.ANONYMOUS) {
				icon.setContributorDisplayName(null);
			}
			changed = true;
		}
		if (request.contributorDisplayName() != null) {
			icon.setContributorDisplayName(blankToNull(request.contributorDisplayName()));
			changed = true;
		}
		if (request.contactEmail() != null) {
			icon.setContactEmail(blankToNull(request.contactEmail()));
			changed = true;
		}

		validateDateRange(icon);
		return changed;
	}

	private void validateDateRange(Icon icon) {
		LocalDate start = icon.getArtworkDateStart();
		LocalDate end = icon.getArtworkDateEnd();
		if (start != null && end != null && end.isBefore(start)) {
			throw new IllegalArgumentException("Artwork end date cannot be before the start date");
		}
	}

	private void appendReview(Icon icon, User reviewer, ReviewAction action, PublicationStatus fromStatus,
			PublicationStatus toStatus, String notes) {
		ContributionReview review = new ContributionReview();
		review.setIcon(icon);
		review.setReviewer(reviewer);
		review.setAction(action);
		review.setFromStatus(fromStatus);
		review.setToStatus(toStatus);
		review.setNotes(notes);
		contributionReviewRepository.save(review);
	}

	private String notesOf(SubmissionRequestDto request) {
		return request == null ? null : blankToNull(request.notes());
	}

	private String blankToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

}
