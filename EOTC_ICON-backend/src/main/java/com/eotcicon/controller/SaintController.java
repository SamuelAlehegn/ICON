package com.eotcicon.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eotcicon.service.SaintService;
import com.eotcicon.web.dto.common.PageResponse;
import com.eotcicon.web.dto.saint.SaintDetailDto;
import com.eotcicon.web.dto.saint.SaintSummaryDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/saints")
@Tag(name = "Saints", description = "Public saint endpoints")
public class SaintController {

	private final SaintService saintService;

	public SaintController(SaintService saintService) {
		this.saintService = saintService;
	}

	@GetMapping
	@Operation(summary = "List saints")
	public PageResponse<SaintSummaryDto> getSaints(
			@Parameter(description = "Page number") @RequestParam(value = "page", defaultValue = "0") int page,
			@Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "12") int size) {
		org.springframework.data.domain.Page<SaintSummaryDto> result = saintService.getSaints(page, size);
		return PageResponse.of(result);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get saint by ID")
	public SaintDetailDto getSaint(@Parameter(description = "Saint ID") @PathVariable("id") String id) {
		return saintService.getSaint(id);
	}
}
