package com.eotcicon.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.eotcicon.domain.Icon;
import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.exception.ResourceNotFoundException;
import com.eotcicon.repository.IconRepository;

@Service
@Transactional(readOnly = true)
public class IconService {

	private final IconRepository iconRepository;

	public IconService(IconRepository iconRepository) {
		this.iconRepository = iconRepository;
	}

	public Page<Icon> getIcons(String q, String region, UUID churchId, UUID saintId, String category,
			String sort, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, buildSort(sort));
		PublicationStatus status = PublicationStatus.APPROVED;

		boolean hasTextSearch = StringUtils.hasText(q);
		boolean hasRegion = StringUtils.hasText(region);

		if (hasTextSearch && !hasRegion && churchId == null && saintId == null && !StringUtils.hasText(category)) {
			return iconRepository.searchPublished(status, q.trim(), pageable);
		}

		if (hasRegion) {
			Page<Icon> filtered = iconRepository.filterPublished(status, churchId, category, saintId, pageable);
			List<Icon> content = filtered.getContent().stream()
					.filter(icon -> icon.getChurch() != null && region.equalsIgnoreCase(icon.getChurch().getRegion()))
					.toList();
			return new org.springframework.data.domain.PageImpl<>(content, pageable, filtered.getTotalElements());
		}

		if (hasTextSearch) {
			Page<Icon> searched = iconRepository.searchPublished(status, q.trim(), pageable);
			if (churchId != null || saintId != null || StringUtils.hasText(category)) {
				List<Icon> content = searched.getContent().stream()
						.filter(icon -> (churchId == null || (icon.getChurch() != null && churchId.equals(icon.getChurch().getId())))
								&& (saintId == null || icon.getSubjects() != null && icon.getSubjects().stream()
										.anyMatch(s -> s.getSaint() != null && saintId.equals(s.getSaint().getId())))
								&& (category == null || (icon.getCategory() != null && icon.getCategory().equalsIgnoreCase(category))))
						.toList();
				return new org.springframework.data.domain.PageImpl<>(content, pageable, content.size());
			}
			return searched;
		}

		if (churchId != null || saintId != null || StringUtils.hasText(category)) {
			return iconRepository.filterPublished(status, churchId, category, saintId, pageable);
		}

		return iconRepository.findByPublicationStatus(status, pageable);
	}

	public Icon getApprovedIcon(String idOrSlug) {
		UUID id = tryParseUuid(idOrSlug);
		if (id != null) {
			return iconRepository.findById(id)
					.filter(icon -> icon.getPublicationStatus() == PublicationStatus.APPROVED)
					.orElseThrow(() -> new ResourceNotFoundException("Icon not found"));
		}
		Icon icon = iconRepository.findBySlug(idOrSlug)
				.orElseThrow(() -> new ResourceNotFoundException("Icon not found"));
		if (icon.getPublicationStatus() != PublicationStatus.APPROVED) {
			throw new ResourceNotFoundException("Icon not found");
		}
		return icon;
	}

	private Sort buildSort(String sort) {
		if (!StringUtils.hasText(sort)) {
			sort = "newest";
		}
		return switch (sort.toLowerCase()) {
			case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
			case "title" -> Sort.by(Sort.Direction.ASC, "title");
			case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
			default -> Sort.by(Sort.Direction.DESC, "createdAt");
		};
	}

	private UUID tryParseUuid(String value) {
		try {
			return UUID.fromString(value);
		} catch (Exception ex) {
			return null;
		}
	}
}
