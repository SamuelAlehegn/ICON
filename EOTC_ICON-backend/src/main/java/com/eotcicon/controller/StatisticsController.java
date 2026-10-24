package com.eotcicon.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eotcicon.service.StatisticsService;
import com.eotcicon.web.dto.stats.ArchiveStatsDto;

@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

	private final StatisticsService statisticsService;

	public StatisticsController(StatisticsService statisticsService) {
		this.statisticsService = statisticsService;
	}

	@GetMapping
	public ArchiveStatsDto getStatistics() {
		return statisticsService.getStatistics();
	}
}
