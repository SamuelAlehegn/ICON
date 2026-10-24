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

@RestController
@RequestMapping("/api/v1/saints")
public class SaintController {

	private final SaintService saintService;

	public SaintController(SaintService saintService) {
		this.saintService = saintService;
	}

	@GetMapping
	public PageResponse<SaintSummaryDto> getSaints(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "12") int size) {
		org.springframework.data.domain.Page<SaintSummaryDto> result = saintService.getSaints(page, size);
		return PageResponse.of(result);
	}

	@GetMapping("/{id}")
	public SaintDetailDto getSaint(@PathVariable("id") String id) {
		return saintService.getSaint(id);
	}
}
