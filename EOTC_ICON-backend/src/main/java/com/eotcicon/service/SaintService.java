package com.eotcicon.service;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eotcicon.domain.Saint;
import com.eotcicon.domain.Icon;
import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.domain.enums.RecordStatus;
import com.eotcicon.exception.ResourceNotFoundException;
import com.eotcicon.repository.IconRepository;
import com.eotcicon.repository.SaintRepository;
import com.eotcicon.web.dto.church.ChurchSummaryDto;
import com.eotcicon.web.dto.icon.IconSummaryDto;
import com.eotcicon.web.dto.saint.SaintDetailDto;
import com.eotcicon.web.dto.saint.SaintSummaryDto;
import com.eotcicon.web.mapper.ChurchMapper;
import com.eotcicon.web.mapper.IconMapper;
import com.eotcicon.web.mapper.SaintMapper;

@Service
@Transactional(readOnly = true)
public class SaintService {

	private final SaintRepository saintRepository;
	private final IconRepository iconRepository;
	private final Function<String, String> mediaUrlResolver;

	public SaintService(SaintRepository saintRepository, IconRepository iconRepository,
			Function<String, String> mediaUrlResolver) {
		this.saintRepository = saintRepository;
		this.iconRepository = iconRepository;
		this.mediaUrlResolver = mediaUrlResolver;
	}

	public Page<SaintSummaryDto> getSaints(int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		List<Saint> saints = saintRepository.findAllByStatus(RecordStatus.ACTIVE);
		// For public list, return as simple list wrapped in page (no pagination constraints in repo yet)
		List<SaintSummaryDto> dtos = saints.stream()
				.map(s -> SaintMapper.toSummary(s, countIconsForSaint(s)))
				.collect(Collectors.toList());
		int start = Math.min((int) pageable.getOffset(), dtos.size());
		int end = Math.min(start + pageable.getPageSize(), dtos.size());
		List<SaintSummaryDto> pageContent = dtos.subList(start, end);
		return new PageImpl<>(pageContent, pageable, dtos.size());
	}

	public SaintDetailDto getSaint(String idOrSlug) {
		Saint saint = findActiveSaint(idOrSlug);
		Page<Icon> iconPage = iconRepository.filterPublished(PublicationStatus.APPROVED, null, null, saint.getId(),
				PageRequest.of(0, 1000));
		List<IconSummaryDto> depictions = iconPage.getContent().stream()
				.<IconSummaryDto> map(icon -> IconMapper.toSummary(icon, mediaUrlResolver))
				.toList();
		// Collect churches from depictions
		java.util.Map<UUID, ChurchSummaryDto> churchMap = new java.util.LinkedHashMap<>();
		for (Icon icon : iconPage.getContent()) {
			if (icon.getChurch() != null) {
				com.eotcicon.domain.Church c = icon.getChurch();
				if (!churchMap.containsKey(c.getId())) {
					churchMap.put(c.getId(), ChurchMapper.toSummary(c));
				}
			}
		}
		List<ChurchSummaryDto> churches = new java.util.ArrayList<>(churchMap.values());
		return SaintMapper.toDetail(saint, depictions, churches);
	}

	private Saint findActiveSaint(String idOrSlug) {
		UUID id = tryParseUuid(idOrSlug);
		if (id != null) {
			Saint saint = saintRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("Saint not found"));
			if (saint.getStatus() != RecordStatus.ACTIVE) {
				throw new ResourceNotFoundException("Saint not found");
			}
			return saint;
		}
		Saint saint = saintRepository.findBySlug(idOrSlug)
				.orElseThrow(() -> new ResourceNotFoundException("Saint not found"));
		if (saint.getStatus() != RecordStatus.ACTIVE) {
			throw new ResourceNotFoundException("Saint not found");
		}
		return saint;
	}

	private Long countIconsForSaint(Saint saint) {
		Page<Icon> page = iconRepository.filterPublished(PublicationStatus.APPROVED, null, null, saint.getId(),
				PageRequest.of(0, 1));
		return page.getTotalElements();
	}

	private UUID tryParseUuid(String value) {
		try {
			return UUID.fromString(value);
		} catch (Exception ex) {
			return null;
		}
	}
}
