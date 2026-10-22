package com.eotcicon.domain;

import java.math.BigDecimal;
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
 * A church that icons are linked to. Duplicates are resolved with
 * {@code mergedInto} instead of being deleted, so the history survives.
 */
@Getter
@Setter
@Entity
@Table(name = "church", uniqueConstraints = @UniqueConstraint(name = "uq_church_slug", columnNames = "slug"))
public class Church extends BaseEntity {

	@Column(name = "name", nullable = false, length = 200)
	private String name;

	@Column(name = "slug", nullable = false, length = 220)
	private String slug;

	@Column(name = "region", length = 120)
	private String region;

	@Column(name = "town", length = 120)
	private String town;

	@Column(name = "country", nullable = false, length = 120)
	private String country = "Ethiopia";

	@Column(name = "latitude", precision = 9, scale = 6)
	private BigDecimal latitude;

	@Column(name = "longitude", precision = 9, scale = 6)
	private BigDecimal longitude;

	@Column(name = "description")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private RecordStatus status = RecordStatus.ACTIVE;

	/**
	 * Set when this record was merged into a canonical one.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "merged_into_id", foreignKey = @ForeignKey(name = "fk_church_merged_into"))
	private Church mergedInto;

	@OneToMany(mappedBy = "church", fetch = FetchType.LAZY)
	private List<ChurchAlias> aliases = new ArrayList<>();

	@OneToMany(mappedBy = "church", fetch = FetchType.LAZY)
	private List<Icon> icons = new ArrayList<>();

}
