package com.eotcicon.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.domain.enums.RecordStatus;
import com.eotcicon.repository.ChurchRepository;
import com.eotcicon.repository.IconRepository;
import com.eotcicon.repository.SaintRepository;
import com.eotcicon.repository.UserRepository;
import com.eotcicon.domain.enums.UserRole;
import com.eotcicon.web.dto.stats.ArchiveStatsDto;
import com.eotcicon.web.mapper.ArchiveStatsMapper;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

	private final IconRepository iconRepository;
	private final ChurchRepository churchRepository;
	private final SaintRepository saintRepository;
	private final UserRepository userRepository;

	public StatisticsService(IconRepository iconRepository, ChurchRepository churchRepository,
			SaintRepository saintRepository, UserRepository userRepository) {
		this.iconRepository = iconRepository;
		this.churchRepository = churchRepository;
		this.saintRepository = saintRepository;
		this.userRepository = userRepository;
	}

	public ArchiveStatsDto getStatistics() {
		long publishedIcons = iconRepository.countByPublicationStatus(PublicationStatus.APPROVED);
		long churches = churchRepository.countByStatus(RecordStatus.ACTIVE);
		long saints = saintRepository.countByStatus(RecordStatus.ACTIVE);
		long regions = churchRepository.countDistinctRegionsByStatus(RecordStatus.ACTIVE);
		long contributors = userRepository.countByRole(UserRole.CONTRIBUTOR);
		boolean sampleData = false;
		return ArchiveStatsMapper.toDto(publishedIcons, churches, saints, regions, contributors, sampleData);
	}
}
