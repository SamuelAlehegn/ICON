package com.eotcicon.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.eotcicon.domain.Icon;
import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.service.IconService;
import com.eotcicon.service.StatisticsService;
import com.eotcicon.web.dto.common.PageResponse;
import com.eotcicon.web.dto.icon.IconDetailDto;
import com.eotcicon.web.dto.icon.IconSummaryDto;
import com.eotcicon.web.dto.stats.ArchiveStatsDto;
import com.eotcicon.web.mapper.IconMapper;

@ExtendWith(MockitoExtension.class)
class PublicIconStatisticsUnitTest {

	@Mock
	private IconService iconService;

	@Mock
	private StatisticsService statisticsService;

	private final Function<String, String> mediaUrlResolver = s -> s;

	@Test
	void iconController_getIcons_returnsMappedPage() {
		Icon icon = new Icon();
		icon.setTitle("Test");
		icon.setSlug("test");
		icon.setPublicationStatus(PublicationStatus.APPROVED);
		Page<Icon> page = new PageImpl<>(List.of(icon), PageRequest.of(0, 12), 1);
		org.mockito.Mockito.when(iconService.getIcons(null, null, null, null, null, null, 0, 12)).thenReturn(page);

		IconController controller = new IconController(iconService, mediaUrlResolver);
		PageResponse<IconSummaryDto> result = controller.getIcons(null, null, null, null, null, null, 0, 12);
		assertNotNull(result);
		assertEquals(1, result.content().size());
	}

	@Test
	void iconController_getIcon_returnsDetail() {
		Icon icon = new Icon();
		icon.setTitle("Detail");
		icon.setSlug("detail");
		icon.setPublicationStatus(PublicationStatus.APPROVED);
		org.mockito.Mockito.when(iconService.getApprovedIcon("detail")).thenReturn(icon);

		IconController controller = new IconController(iconService, mediaUrlResolver);
		IconDetailDto dto = controller.getIcon("detail");
		assertNotNull(dto);
		assertEquals("Detail", dto.title());
	}

	@Test
	void statisticsController_getStatistics_returnsStats() {
		ArchiveStatsDto stats = new ArchiveStatsDto(10, 5, 3, 2, 0, false);
		org.mockito.Mockito.when(statisticsService.getStatistics()).thenReturn(stats);

		StatisticsController controller = new StatisticsController(statisticsService);
		ArchiveStatsDto result = controller.getStatistics();
		assertNotNull(result);
		assertEquals(10, result.publishedIcons());
		assertEquals(5, result.churches());
		assertEquals(3, result.saints());
		assertEquals(2, result.regions());
	}
}
