package com.eotcicon.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.eotcicon.domain.Church;
import com.eotcicon.domain.ContributionReview;
import com.eotcicon.domain.Icon;
import com.eotcicon.domain.IconImage;
import com.eotcicon.domain.IconSubject;
import com.eotcicon.domain.User;
import com.eotcicon.domain.enums.AttributionPreference;
import com.eotcicon.domain.enums.FigureCertainty;
import com.eotcicon.domain.enums.PublicationStatus;
import com.eotcicon.domain.enums.ReviewAction;
import com.eotcicon.repository.ChurchRepository;
import com.eotcicon.repository.ContributionReviewRepository;
import com.eotcicon.repository.IconImageRepository;
import com.eotcicon.repository.IconRepository;
import com.eotcicon.repository.UserRepository;
import com.eotcicon.service.storage.StorageService;
import com.eotcicon.web.dto.contribution.ContributionCreatedDto;
import com.eotcicon.web.mapper.ContributionMapper;

@Service
@Transactional
public class ContributionService {

	private final IconRepository iconRepository;
	private final IconImageRepository iconImageRepository;
	private final ContributionReviewRepository contributionReviewRepository;
	private final ChurchRepository churchRepository;
	private final UserRepository userRepository;
	private final StorageService storageService;
	private final ImageValidationService imageValidationService;
	private final ThumbnailService thumbnailService;
	private final RateLimitService rateLimitService;

	public ContributionService(IconRepository iconRepository, IconImageRepository iconImageRepository,
			ContributionReviewRepository contributionReviewRepository, ChurchRepository churchRepository,
			UserRepository userRepository, StorageService storageService,
			ImageValidationService imageValidationService, ThumbnailService thumbnailService,
			RateLimitService rateLimitService) {
		this.iconRepository = iconRepository;
		this.iconImageRepository = iconImageRepository;
		this.contributionReviewRepository = contributionReviewRepository;
		this.churchRepository = churchRepository;
		this.userRepository = userRepository;
		this.storageService = storageService;
		this.imageValidationService = imageValidationService;
		this.thumbnailService = thumbnailService;
		this.rateLimitService = rateLimitService;
	}

	public ContributionCreatedDto submitContribution(ContributionRequest request) {
		if (request.images() == null || request.images().isEmpty()) {
			throw new IllegalArgumentException("At least one image is required");
		}
		if (!Boolean.TRUE.equals(request.consent())) {
			throw new IllegalArgumentException("Consent is required");
		}
		if (!StringUtils.hasText(request.churchName())) {
			throw new IllegalArgumentException("Church name is required");
		}

		String rateLimitKey = request.contactEmail();
		if (!StringUtils.hasText(rateLimitKey)) {
			rateLimitKey = request.contributorDisplayName() != null ? request.contributorDisplayName() : "anonymous";
		}
		if (!rateLimitService.allowRequest(rateLimitKey)) {
			throw new IllegalArgumentException("Too many requests. Please try again later.");
		}

		for (MultipartFile image : request.images()) {
			imageValidationService.validate(image);
		}

		Icon icon = new Icon();
		String referenceCode = generateReferenceCode();
		icon.setReferenceCode(referenceCode);
		icon.setSlug(generateSlug(request.title() != null ? request.title() : request.churchName()));
		icon.setTitle(StringUtils.hasText(request.title()) ? request.title() : request.churchName());
		icon.setDescription(request.description());
		icon.setHistoricalNotes(request.historicalNotes());
		icon.setCategory(request.category());
		icon.setFigureCertainty(StringUtils.hasText(request.saintName()) ? FigureCertainty.IDENTIFIED : FigureCertainty.UNKNOWN);
		icon.setPublicationStatus(PublicationStatus.PENDING);
		icon.setAttributionPreference(parseAttribution(request.attributionPreference()));
		icon.setContributorDisplayName(resolveDisplayName(request));
		icon.setContactEmail(request.contactEmail());

		if (StringUtils.hasText(request.capturedDate())) {
			try {
				icon.setCaptureDate(LocalDate.parse(request.capturedDate()));
			} catch (Exception ignored) {
			}
		}

		Church church = findOrCreateChurch(request);
		icon.setChurch(church);

		icon = iconRepository.save(icon);

		List<IconImage> savedImages = new ArrayList<>();
		int sortOrder = 0;
		for (MultipartFile image : request.images()) {
			try {
				String ext = getExtension(image.getOriginalFilename(), image.getContentType());
				String objectKey = "icons/" + icon.getId() + "/" + UUID.randomUUID() + "." + ext;
				storageService.store(image, objectKey);
				thumbnailService.generateThumbnail(objectKey);

				IconImage iconImage = new IconImage();
				iconImage.setIcon(icon);
				iconImage.setStorageKey(objectKey);
				iconImage.setThumbnailStorageKey("icons/" + icon.getId() + "/thumb_" + UUID.randomUUID() + "." + ext);
				iconImage.setContentType(image.getContentType());
				iconImage.setFileSizeBytes(image.getSize());
				iconImage.setChecksumSha256(computeChecksum(image));
				iconImage.setAltText(icon.getTitle());
				iconImage.setPrimary(sortOrder == 0);
				iconImage.setSortOrder(sortOrder++);
				savedImages.add(iconImageRepository.save(iconImage));
			} catch (IOException e) {
				throw new IllegalArgumentException("Failed to store image");
			}
		}
		icon.setImages(savedImages);

		if (StringUtils.hasText(request.saintName())) {
			IconSubject subject = new IconSubject();
			subject.setIcon(icon);
			subject.setSubjectLabel(request.saintName());
			subject.setFigureCertainty(FigureCertainty.IDENTIFIED);
			subject.setPrimary(true);
			subject.setSortOrder(0);
			icon.getSubjects().add(subject);
		}

		ContributionReview review = new ContributionReview();
		review.setIcon(icon);
		review.setReviewer(resolveSystemReviewer());
		review.setAction(ReviewAction.SUBMIT);
		review.setToStatus(PublicationStatus.PENDING);
		contributionReviewRepository.save(review);

		iconRepository.save(icon);
		return ContributionMapper.toCreated(icon, "Thank you — your submission is awaiting review.");
	}

	public Icon getByReferenceCode(String referenceCode) {
		return iconRepository.findByReferenceCode(referenceCode)
				.orElseThrow(() -> new com.eotcicon.exception.ResourceNotFoundException("Submission not found"));
	}

	private String generateReferenceCode() {
		String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
		String code = uuid.substring(0, 12);
		while (iconRepository.existsByReferenceCode("EOTC-" + code)) {
			uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
			code = uuid.substring(0, 12);
		}
		return "EOTC-" + code;
	}

	private String generateSlug(String base) {
		if (!StringUtils.hasText(base)) {
			base = "icon";
		}
		String slug = base.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
		String candidate = slug;
		int counter = 1;
		while (iconRepository.existsBySlug(candidate)) {
			candidate = slug + "-" + counter++;
		}
		return candidate;
	}

	private String getExtension(String filename, String contentType) {
		if (StringUtils.hasText(filename) && filename.contains(".")) {
			String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
			if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("webp") || ext.equals("tif")
					|| ext.equals("tiff")) {
				return ext.equals("jpg") ? "jpg" : ext;
			}
		}
		if (contentType == null) {
			return "jpg";
		}
		String type = contentType.toLowerCase();
		if (type.contains("png")) {
			return "png";
		}
		if (type.contains("webp")) {
			return "webp";
		}
		if (type.contains("tiff")) {
			return "tif";
		}
		return "jpg";
	}

	private String computeChecksum(MultipartFile file) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(file.getBytes());
			return HexFormat.of().formatHex(hash);
		} catch (Exception e) {
			return UUID.randomUUID().toString().replace("-", "");
		}
	}

	private AttributionPreference parseAttribution(String value) {
		if (!StringUtils.hasText(value)) {
			return AttributionPreference.DISPLAY_NAME;
		}
		try {
			return AttributionPreference.valueOf(value.toUpperCase());
		} catch (Exception e) {
			return AttributionPreference.DISPLAY_NAME;
		}
	}

	private String resolveDisplayName(ContributionRequest request) {
		if (parseAttribution(request.attributionPreference()) == AttributionPreference.ANONYMOUS) {
			return null;
		}
		return StringUtils.hasText(request.contributorDisplayName()) ? request.contributorDisplayName()
				: "Anonymous";
	}

	private Church findOrCreateChurch(ContributionRequest request) {
		if (!StringUtils.hasText(request.churchName())) {
			return null;
		}
		List<Church> all = churchRepository.findAll();
		for (Church church : all) {
			if (church.getName() != null && church.getName().equalsIgnoreCase(request.churchName())) {
				if (StringUtils.hasText(request.town()) && !StringUtils.hasText(church.getTown())) {
					church.setTown(request.town());
				}
				if (StringUtils.hasText(request.region()) && !StringUtils.hasText(church.getRegion())) {
					church.setRegion(request.region());
				}
				if (StringUtils.hasText(request.country()) && !StringUtils.hasText(church.getCountry())) {
					church.setCountry(request.country());
				}
				return churchRepository.save(church);
			}
		}
		Church church = new Church();
		church.setName(request.churchName());
		church.setSlug(generateChurchSlug(request.churchName()));
		church.setTown(request.town());
		church.setRegion(request.region());
		church.setCountry(StringUtils.hasText(request.country()) ? request.country() : "Ethiopia");
		return churchRepository.save(church);
	}

	private String generateChurchSlug(String base) {
		String slug = base.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
		String candidate = slug;
		int counter = 1;
		while (churchRepository.existsBySlug(candidate)) {
			candidate = slug + "-" + counter++;
		}
		return candidate;
	}

	private User resolveSystemReviewer() {
		return userRepository.findAll().stream().findFirst().orElse(null);
	}

	public record ContributionRequest(
			List<MultipartFile> images,
			String churchName,
			String town,
			String region,
			String country,
			Double latitude,
			Double longitude,
			String saintName,
			String category,
			String title,
			String description,
			String historicalNotes,
			String capturedDate,
			String attributionPreference,
			String contributorDisplayName,
			String contactEmail,
			Boolean consent) {
	}
}
