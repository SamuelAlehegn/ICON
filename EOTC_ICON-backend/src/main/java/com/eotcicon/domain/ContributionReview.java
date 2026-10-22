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

import lombok.Getter;
import lombok.Setter;

/**
 * An immutable moderation trail of an icon contribution. Rows are only ever
 * inserted; {@code fromStatus} and {@code toStatus} record the state
 * transition that the review caused.
 */
@Getter
@Setter
@Entity
@Table(name = "contribution_review")
public class ContributionReview extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "icon_id", nullable = false, foreignKey = @ForeignKey(name = "fk_contribution_review_icon"))
	private Icon icon;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reviewer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_contribution_review_reviewer"))
	private User reviewer;

	@Enumerated(EnumType.STRING)
	@Column(name = "action", nullable = false, length = 30)
	private ReviewAction action;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", length = 20)
	private PublicationStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", length = 20)
	private PublicationStatus toStatus;

	@Column(name = "notes")
	private String notes;

}
