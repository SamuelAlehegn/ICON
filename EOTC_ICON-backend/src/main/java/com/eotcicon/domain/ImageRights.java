package com.eotcicon.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

/**
 * The rights and attribution statement of a single image. The licence of the
 * artwork and the licence of the photograph are distinct records, so the
 * statement that is legally binding for the image is stored next to the image.
 */
@Getter
@Setter
@Entity
@Table(name = "image_rights", uniqueConstraints = @UniqueConstraint(name = "uq_image_rights_image", columnNames = "icon_image_id"))
public class ImageRights extends BaseEntity {

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "icon_image_id", nullable = false, foreignKey = @ForeignKey(name = "fk_image_rights_image"))
	private IconImage image;

	@Column(name = "rights_holder", length = 200)
	private String rightsHolder;

	@Enumerated(EnumType.STRING)
	@Column(name = "license_type", nullable = false, length = 40)
	private LicenseType licenseType = LicenseType.UNKNOWN;

	@Column(name = "license_url", length = 500)
	private String licenseUrl;

	@Column(name = "license_notes")
	private String licenseNotes;

	@Column(name = "attribution_text", length = 500)
	private String attributionText;

	@Column(name = "copyright_year")
	private Integer copyrightYear;

	@Column(name = "is_verified", nullable = false)
	private boolean isVerified = false;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "verified_by", foreignKey = @ForeignKey(name = "fk_image_rights_verifier"))
	private User verifiedBy;

	@Column(name = "verified_at")
	private Instant verifiedAt;

}
