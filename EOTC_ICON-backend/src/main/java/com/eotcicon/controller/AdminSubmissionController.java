package com.eotcicon.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.service.AdminSubmissionService;
import com.eotcicon.web.dto.common.PageResponse;
import com.eotcicon.web.dto.contribution.AdminSubmissionDetailDto;
import com.eotcicon.web.dto.contribution.ReviewDecisionRequest;
import com.eotcicon.web.dto.contribution.ReviewDecisionResponse;
import com.eotcicon.web.dto.contribution.SubmissionSummaryDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/submissions")
public class AdminSubmissionController {

	private final AdminSubmissionService submissionService;

	public AdminSubmissionController(AdminSubmissionService submissionService) {
		this.submissionService = submissionService;
	}

	@GetMapping
	public PageResponse<SubmissionSummaryDto> getQueue(
			@RequestParam(required = false) PublicationStatus status,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "12") int size) {
		Page<SubmissionSummaryDto> result = submissionService.getQueue(status, q, page, size);
		return PageResponse.of(result);
	}

	@GetMapping("/{id}")
	public AdminSubmissionDetailDto getSubmission(@PathVariable UUID id) {
		return submissionService.getSubmission(id);
	}

	@PostMapping("/{id}/decision")
	@ResponseStatus(HttpStatus.OK)
	public ReviewDecisionResponse decide(@PathVariable UUID id, @Valid @RequestBody ReviewDecisionRequest request) {
		return submissionService.decide(id, request);
	}
}
