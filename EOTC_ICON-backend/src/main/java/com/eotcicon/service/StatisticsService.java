package com.eotcicon.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.domain.enums.RecordStatus;
import com.eotcicon.repository.ChurchRepository;
import com.eotcicon.repository.IconRepository;
import com.eotcicon.repository.SaintRepository;
import com.eotcicon.web.dto.stats.ArchiveStatsDto;
import com.eotcicon.web.mapper.ArchiveStatsMapper;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

	private final IconRepository iconRepository;
	private final ChurchRepository churchRepository;
	private final SaintRepository saintRepository;

	public StatisticsService(IconRepository iconRepository, ChurchRepository churchRepository,
			SaintRepository saintRepository) {
		this.iconRepository = iconRepository;
		this.churchRepository = churchRepository;
		this.saintRepository = saintRepository;
	}

	public ArchiveStatsDto getStatistics() {
		long publishedIcons = iconRepository.countByPublicationStatus(PublicationStatus.APPROVED);
		long churches = churchRepository.countByStatus(RecordStatus.ACTIVE);
		long saints = saintRepository.countByStatus(RecordStatus.ACTIVE);
		long regions = churchRepository.countDistinctRegionsByStatus(RecordStatus.ACTIVE);
		long contributors = 0;
		boolean sampleData = false;
		return ArchiveStatsMapper.toDto(publishedIcons, churches, saints, regions, contributors, sampleData);
	}
}
