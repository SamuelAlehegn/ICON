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

import com.eotcicon.domain.Church;
import com.eotcicon.domain.Icon;
import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.domain.enums.RecordStatus;
import com.eotcicon.exception.ResourceNotFoundException;
import com.eotcicon.repository.ChurchRepository;
import com.eotcicon.repository.IconRepository;
import com.eotcicon.web.dto.church.ChurchDetailDto;
import com.eotcicon.web.dto.church.ChurchSummaryDto;
import com.eotcicon.web.dto.icon.IconSummaryDto;
import com.eotcicon.web.mapper.ChurchMapper;
import com.eotcicon.web.mapper.IconMapper;

@Service
@Transactional(readOnly = true)
public class ChurchService {

	private final ChurchRepository churchRepository;
	private final IconRepository iconRepository;
	private final Function<String, String> mediaUrlResolver;

	public ChurchService(ChurchRepository churchRepository, IconRepository iconRepository,
			Function<String, String> mediaUrlResolver) {
		this.churchRepository = churchRepository;
		this.iconRepository = iconRepository;
		this.mediaUrlResolver = mediaUrlResolver;
	}

	public Page<ChurchSummaryDto> getChurches(int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		List<Church> churches = churchRepository.findAllByStatus(RecordStatus.ACTIVE);
		List<ChurchSummaryDto> dtos = churches.stream().map(ChurchMapper::toSummary).collect(Collectors.toList());
		int start = Math.min((int) pageable.getOffset(), dtos.size());
		int end = Math.min(start + pageable.getPageSize(), dtos.size());
		List<ChurchSummaryDto> pageContent = dtos.subList(start, end);
		return new PageImpl<>(pageContent, pageable, dtos.size());
	}

	public ChurchDetailDto getChurch(String idOrSlug) {
		Church church = findActiveChurch(idOrSlug);
		Page<Icon> iconPage = iconRepository.findByPublicationStatusAndChurch_Id(PublicationStatus.APPROVED,
				church.getId(), PageRequest.of(0, 1000));
		List<IconSummaryDto> icons = iconPage.getContent().stream()
				.<IconSummaryDto> map(icon -> IconMapper.toSummary(icon, mediaUrlResolver))
				.toList();
		return ChurchMapper.toDetail(church, icons);
	}

	private Church findActiveChurch(String idOrSlug) {
		UUID id = tryParseUuid(idOrSlug);
		if (id != null) {
			Church church = churchRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("Church not found"));
			if (church.getStatus() != RecordStatus.ACTIVE) {
				throw new ResourceNotFoundException("Church not found");
			}
			return church;
		}
		Church church = churchRepository.findBySlug(idOrSlug)
				.orElseThrow(() -> new ResourceNotFoundException("Church not found"));
		if (church.getStatus() != RecordStatus.ACTIVE) {
			throw new ResourceNotFoundException("Church not found");
		}
		return church;
	}

	private UUID tryParseUuid(String value) {
		try {
			return UUID.fromString(value);
		} catch (Exception ex) {
			return null;
		}
	}
}
