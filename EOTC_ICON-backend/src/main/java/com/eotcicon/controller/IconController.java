package com.eotcicon.controller;

import java.util.UUID;
import java.util.function.Function;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eotcicon.domain.Icon;
import com.eotcicon.service.IconService;
import com.eotcicon.web.dto.common.PageResponse;
import com.eotcicon.web.dto.icon.IconDetailDto;
import com.eotcicon.web.dto.icon.IconSummaryDto;
import com.eotcicon.web.mapper.IconMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/icons")
@Tag(name = "Icons", description = "Public icon endpoints")
public class IconController {

	private final IconService iconService;
	private final Function<String, String> mediaUrlResolver;

	public IconController(IconService iconService, Function<String, String> mediaUrlResolver) {
		this.iconService = iconService;
		this.mediaUrlResolver = mediaUrlResolver;
	}

	@GetMapping
	@Operation(summary = "List approved icons", description = "Returns paginated approved icons with optional filters")
	public PageResponse<IconSummaryDto> getIcons(
			@Parameter(description = "Search query") @RequestParam(value = "q", required = false) String q,
			@Parameter(description = "Region filter") @RequestParam(value = "region", required = false) String region,
			@Parameter(description = "Church ID filter") @RequestParam(value = "churchId", required = false) UUID churchId,
			@Parameter(description = "Saint ID filter") @RequestParam(value = "saintId", required = false) UUID saintId,
			@Parameter(description = "Category filter") @RequestParam(value = "category", required = false) String category,
			@Parameter(description = "Sort order") @RequestParam(value = "sort", required = false) String sort,
			@Parameter(description = "Page number") @RequestParam(value = "page", defaultValue = "0") int page,
			@Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "12") int size) {
		org.springframework.data.domain.Page<Icon> result = iconService.getIcons(q, region, churchId, saintId,
				category, sort, page, size);
		return IconMapper.toSummaryPage(result, mediaUrlResolver);
	}

	@GetMapping("/{idOrSlug}")
	@Operation(summary = "Get icon by ID or slug", description = "Returns approved icon details by ID or slug")
	public IconDetailDto getIcon(@Parameter(description = "Icon ID or slug") @PathVariable("idOrSlug") String idOrSlug) {
		Icon icon = iconService.getApprovedIcon(idOrSlug);
		return IconMapper.toDetail(icon, mediaUrlResolver);
	}
}
