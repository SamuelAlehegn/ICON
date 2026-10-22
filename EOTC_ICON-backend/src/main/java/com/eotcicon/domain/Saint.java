package com.eotcicon.domain;

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

/**
 * A saint in the Ethiopian Orthodox Tewahedo Church tradition. Amharic and
 * Ge'ez names are first-class columns because they are functionally dependent
 * on the saint, while further alternative spellings live in
 * {@link SaintAlias}.
 */
@Getter
@Setter
@Entity
@Table(name = "saint", uniqueConstraints = @UniqueConstraint(name = "uq_saint_slug", columnNames = "slug"))
public class Saint extends BaseEntity {

	@Column(name = "canonical_name", nullable = false, length = 200)
	private String canonicalName;

	@Column(name = "slug", nullable = false, length = 220)
	private String slug;

	@Column(name = "amharic_name", length = 200)
	private String amharicName;

	@Column(name = "geez_name", length = 200)
	private String geezName;

	@Column(name = "category", length = 80)
	private String category;

	@Column(name = "description")
	private String description;

	@Column(name = "is_verified", nullable = false)
	private boolean isVerified = false;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private RecordStatus status = RecordStatus.ACTIVE;

	/**
	 * Set when this record was merged into a canonical one.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "merged_into_id", foreignKey = @ForeignKey(name = "fk_saint_merged_into"))
	private Saint mergedInto;

	@OneToMany(mappedBy = "saint", fetch = FetchType.LAZY)
	private List<SaintAlias> aliases = new ArrayList<>();

	@OneToMany(mappedBy = "saint", fetch = FetchType.LAZY)
	private List<IconSubject> iconSubjects = new ArrayList<>();

}
