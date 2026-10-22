package com.eotcicon.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * A photograph of an icon. The {@code storageKey} points into the media store
 * (local filesystem or S3/MinIO).
 */
@Getter
@Setter
@Entity
@Table(name = "icon_image", uniqueConstraints = {
		@UniqueConstraint(name = "uq_icon_image_storage_key", columnNames = "storage_key"),
		@UniqueConstraint(name = "uq_icon_image_checksum", columnNames = { "icon_id", "checksum_sha256" }) })
public class IconImage extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "icon_id", nullable = false, foreignKey = @ForeignKey(name = "fk_icon_image_icon"))
	private Icon icon;

	@Column(name = "storage_key", nullable = false, length = 500)
	private String storageKey;

	@Column(name = "thumbnail_storage_key", length = 500)
	private String thumbnailStorageKey;

	@Column(name = "content_type", nullable = false, length = 120)
	private String contentType;

	@Column(name = "file_size_bytes", nullable = false)
	private long fileSizeBytes;

	@Column(name = "checksum_sha256", nullable = false, length = 64)
	private String checksumSha256;

	@Column(name = "width_px")
	private Integer widthPx;

	@Column(name = "height_px")
	private Integer heightPx;

	@Column(name = "alt_text", length = 500)
	private String altText;

	@Column(name = "is_primary", nullable = false)
	private boolean isPrimary = false;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder = 0;

	@Column(name = "captured_at")
	private Instant capturedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "uploaded_by", foreignKey = @ForeignKey(name = "fk_icon_image_uploader"))
	private User uploadedBy;

	/**
	 * The rights statement of this image; there is at most one per image.
	 */
	@OneToOne(mappedBy = "image", fetch = FetchType.LAZY)
	private ImageRights rights;

}
