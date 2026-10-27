package com.eotcicon.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.eotcicon.domain.Icon;
import com.eotcicon.service.ContributionService;
import com.eotcicon.web.dto.contribution.ContributionCreatedDto;
import com.eotcicon.web.dto.contribution.ContributionStatusDto;
import com.eotcicon.web.mapper.ContributionMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/contributions")
@Tag(name = "Contributions", description = "Public contribution endpoints")
public class ContributionController {

	private final ContributionService contributionService;

	public ContributionController(ContributionService contributionService) {
		this.contributionService = contributionService;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Submit a contribution", description = "Anonymous submission with metadata and images")
	public ContributionCreatedDto submit(
			@RequestParam("images") List<MultipartFile> images,
			@RequestParam("churchName") String churchName,
			@RequestParam(value = "town", required = false) String town,
			@RequestParam(value = "region", required = false) String region,
			@RequestParam(value = "country", required = false) String country,
			@RequestParam(value = "latitude", required = false) Double latitude,
			@RequestParam(value = "longitude", required = false) Double longitude,
			@RequestParam(value = "saintName", required = false) String saintName,
			@RequestParam(value = "category", required = false) String category,
			@RequestParam(value = "title", required = false) String title,
			@RequestParam(value = "description", required = false) String description,
			@RequestParam(value = "historicalNotes", required = false) String historicalNotes,
			@RequestParam(value = "capturedDate", required = false) String capturedDate,
			@RequestParam(value = "attributionPreference", required = false) String attributionPreference,
			@RequestParam(value = "contributorDisplayName", required = false) String contributorDisplayName,
			@RequestParam(value = "contactEmail", required = false) String contactEmail,
			@RequestParam("consent") Boolean consent) {
		ContributionService.ContributionRequest request = new ContributionService.ContributionRequest(
				images, churchName, town, region, country, latitude, longitude, saintName, category, title,
				description, historicalNotes, capturedDate, attributionPreference, contributorDisplayName,
				contactEmail, consent);
		return contributionService.submitContribution(request);
	}

	@GetMapping("/status/{referenceCode}")
	@Operation(summary = "Get submission status")
	public ContributionStatusDto getStatus(@PathVariable String referenceCode) {
		Icon icon = contributionService.getByReferenceCode(referenceCode);
		return ContributionMapper.toStatus(icon, icon.getModerationNotes());
	}
}
