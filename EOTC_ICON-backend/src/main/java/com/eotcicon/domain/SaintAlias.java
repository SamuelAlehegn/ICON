package com.eotcicon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

/**
 * Alternative spelling / transliteration of a saint's name.
 */
@Getter
@Setter
@Entity
@Table(name = "saint_alias", uniqueConstraints = @UniqueConstraint(name = "uq_saint_alias_name", columnNames = {
		"saint_id", "name" }))
public class SaintAlias extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "saint_id", nullable = false, foreignKey = @ForeignKey(name = "fk_saint_alias_saint"))
	private Saint saint;

	@Column(name = "name", nullable = false, length = 200)
	private String name;

}
