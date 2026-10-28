package com.eotcicon.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.eotcicon.service.AccountService;
import com.eotcicon.web.dto.contribution.AccountSubmissionDto;
import com.eotcicon.web.dto.contribution.ReviewDecisionResponse;
import com.eotcicon.web.dto.contribution.SubmissionRequestDto;
import com.eotcicon.web.dto.contribution.UpdateSubmissionRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/account")
@Tag(name = "Account", description = "Submission management for the signed-in contributor")
public class AccountController {

	private final AccountService accountService;

	public AccountController(AccountService accountService) {
		this.accountService = accountService;
	}

	@GetMapping("/submissions")
	@Operation(summary = "List my submissions", description = "The caller's submissions with status and review history")
	public List<AccountSubmissionDto> getMySubmissions() {
		return accountService.getMySubmissions();
	}

	@GetMapping("/submissions/{id}")
	@Operation(summary = "Get one of my submissions")
	public AccountSubmissionDto getMySubmission(@PathVariable UUID id) {
		return accountService.getMySubmission(id);
	}

	@RequestMapping(value = "/submissions/{id}", method = { RequestMethod.PATCH, RequestMethod.PUT })
	@Operation(summary = "Update my submission", description = "Updates the editable fields of a pending submission")
	public AccountSubmissionDto updateSubmission(@PathVariable UUID id,
			@Valid @RequestBody UpdateSubmissionRequest request) {
		return accountService.updateSubmission(id, request);
	}

	@PostMapping({ "/submissions/{id}/request-correction", "/submissions/{id}/correction-request" })
	@Operation(summary = "Request a correction", description = "Marks the submission as needing correction")
	public ReviewDecisionResponse requestCorrection(@PathVariable UUID id,
			@Valid @RequestBody(required = false) SubmissionRequestDto request) {
		return accountService.requestCorrection(id, request);
	}

	@PostMapping({ "/submissions/{id}/request-removal", "/submissions/{id}/removal-request" })
	@Operation(summary = "Request removal", description = "Archives the submission and records the request")
	public ReviewDecisionResponse requestRemoval(@PathVariable UUID id,
			@Valid @RequestBody(required = false) SubmissionRequestDto request) {
		return accountService.requestRemoval(id, request);
	}

}
