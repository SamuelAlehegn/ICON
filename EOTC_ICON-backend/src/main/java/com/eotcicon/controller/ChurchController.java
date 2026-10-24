package com.eotcicon.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eotcicon.service.ChurchService;
import com.eotcicon.web.dto.common.PageResponse;
import com.eotcicon.web.dto.church.ChurchDetailDto;
import com.eotcicon.web.dto.church.ChurchSummaryDto;

@RestController
@RequestMapping("/api/v1/churches")
public class ChurchController {

	private final ChurchService churchService;

	public ChurchController(ChurchService churchService) {
		this.churchService = churchService;
	}

	@GetMapping
	public PageResponse<ChurchSummaryDto> getChurches(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "12") int size) {
		org.springframework.data.domain.Page<ChurchSummaryDto> result = churchService.getChurches(page, size);
		return PageResponse.of(result);
	}

	@GetMapping("/{id}")
	public ChurchDetailDto getChurch(@PathVariable("id") String id) {
		return churchService.getChurch(id);
	}
}
