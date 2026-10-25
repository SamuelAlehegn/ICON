package com.eotcicon.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eotcicon.service.StatisticsService;
import com.eotcicon.web.dto.stats.ArchiveStatsDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/statistics")
@Tag(name = "Statistics", description = "Archive statistics endpoints")
public class StatisticsController {

	private final StatisticsService statisticsService;

	public StatisticsController(StatisticsService statisticsService) {
		this.statisticsService = statisticsService;
	}

	@GetMapping
	@Operation(summary = "Get archive statistics", description = "Returns counts of icons, churches, saints and regions")
	public ArchiveStatsDto getStatistics() {
		return statisticsService.getStatistics();
	}
}
