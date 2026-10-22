package com.eotcicon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

/**
 * A figure depicted on an icon. The saint is optional: an icon may show an
 * unidentified figure, in which case {@code subjectLabel} keeps the caption
 * that appears on the artwork and the certainty is {@code UNKNOWN}.
 */
@Getter
@Setter
@Entity
@Table(name = "icon_subject", uniqueConstraints = @UniqueConstraint(name = "uq_icon_subject_label", columnNames = {
		"icon_id", "subject_label" }))
public class IconSubject extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "icon_id", nullable = false, foreignKey = @ForeignKey(name = "fk_icon_subject_icon"))
	private Icon icon;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "saint_id", foreignKey = @ForeignKey(name = "fk_icon_subject_saint"))
	private Saint saint;

	@Column(name = "subject_label", nullable = false, length = 200)
	private String subjectLabel;

	@Enumerated(EnumType.STRING)
	@Column(name = "figure_certainty", nullable = false, length = 20)
	private FigureCertainty figureCertainty = FigureCertainty.UNKNOWN;

	@Column(name = "is_primary", nullable = false)
	private boolean isPrimary = false;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder = 0;

}
