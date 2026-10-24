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

@RestController
@RequestMapping("/api/v1/icons")
public class IconController {

	private final IconService iconService;
	private final Function<String, String> mediaUrlResolver;

	public IconController(IconService iconService, Function<String, String> mediaUrlResolver) {
		this.iconService = iconService;
		this.mediaUrlResolver = mediaUrlResolver;
	}

	@GetMapping
	public PageResponse<IconSummaryDto> getIcons(
			@RequestParam(value = "q", required = false) String q,
			@RequestParam(value = "region", required = false) String region,
			@RequestParam(value = "churchId", required = false) UUID churchId,
			@RequestParam(value = "saintId", required = false) UUID saintId,
			@RequestParam(value = "category", required = false) String category,
			@RequestParam(value = "sort", required = false) String sort,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "12") int size) {
		org.springframework.data.domain.Page<Icon> result = iconService.getIcons(q, region, churchId, saintId,
				category, sort, page, size);
		return IconMapper.toSummaryPage(result, mediaUrlResolver);
	}

	@GetMapping("/{idOrSlug}")
	public IconDetailDto getIcon(@PathVariable("idOrSlug") String idOrSlug) {
		Icon icon = iconService.getApprovedIcon(idOrSlug);
		return IconMapper.toDetail(icon, mediaUrlResolver);
	}
}
