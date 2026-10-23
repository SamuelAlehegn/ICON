package com.eotcicon.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

import com.eotcicon.domain.enums.AttributionPreference;
import com.eotcicon.domain.enums.FigureCertainty;
import com.eotcicon.domain.enums.PublicationStatus;

/**
 * An artwork record and the root of the aggregate.
 */
@Getter
@Setter
@Entity
@Table(name = "icon", uniqueConstraints = { @UniqueConstraint(name = "uq_icon_slug", columnNames = "slug"),
		@UniqueConstraint(name = "uq_icon_reference_code", columnNames = "reference_code") })
public class Icon extends BaseEntity {

	@Column(name = "title", nullable = false, length = 240)
	private String title;

	@Column(name = "slug", nullable = false, length = 260)
	private String slug;

	@Column(name = "reference_code", nullable = false, length = 32)
	private String referenceCode;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "church_id", foreignKey = @ForeignKey(name = "fk_icon_church"))
	private Church church;

	/**
	 * Roll-up of the primary subject's identification certainty.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "figure_certainty", nullable = false, length = 20)
	private FigureCertainty figureCertainty = FigureCertainty.UNKNOWN;

	@Column(name = "description")
	private String description;

	@Column(name = "historical_notes")
	private String historicalNotes;

	@Column(name = "category", length = 80)
	private String category;

	/**
	 * Date the icon was photographed; mirrors the date part of the primary
	 * {@link IconImage#capturedAt}.
	 */
	@Column(name = "capture_date")
	private LocalDate captureDate;

	@Column(name = "artwork_date_start")
	private LocalDate artworkDateStart;

	@Column(name = "artwork_date_end")
	private LocalDate artworkDateEnd;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "contributor_user_id", foreignKey = @ForeignKey(name = "fk_icon_contributor"))
	private User contributor;

	/**
	 * Snapshot of the contributor's display name. It is stored on purpose: an
	 * anonymous submission has no user row and a user may change their name.
	 */
	@Column(name = "contributor_display_name", length = 120)
	private String contributorDisplayName;

	@Enumerated(EnumType.STRING)
	@Column(name = "attribution_preference", nullable = false, length = 20)
	private AttributionPreference attributionPreference = AttributionPreference.DISPLAY_NAME;

	/**
	 * PRIVATE. Never expose it in a public DTO.
	 */
	@Column(name = "contact_email", length = 320)
	private String contactEmail;

	@Enumerated(EnumType.STRING)
	@Column(name = "publication_status", nullable = false, length = 20)
	private PublicationStatus publicationStatus = PublicationStatus.PENDING;

	@Column(name = "moderation_notes")
	private String moderationNotes;

	/**
	 * Stamped when the icon is approved.
	 */
	@Column(name = "published_at")
	private Instant publishedAt;

	@OneToMany(mappedBy = "icon", fetch = FetchType.LAZY)
	private List<IconSubject> subjects = new ArrayList<>();

	@OneToMany(mappedBy = "icon", fetch = FetchType.LAZY)
	private List<IconImage> images = new ArrayList<>();

	@OneToMany(mappedBy = "icon", fetch = FetchType.LAZY)
	private List<ContributionReview> reviews = new ArrayList<>();

}
