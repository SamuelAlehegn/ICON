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
 * Alternative / historical name of a church.
 */
@Getter
@Setter
@Entity
@Table(name = "church_alias", uniqueConstraints = @UniqueConstraint(name = "uq_church_alias_name", columnNames = {
		"church_id", "name" }))
public class ChurchAlias extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "church_id", nullable = false, foreignKey = @ForeignKey(name = "fk_church_alias_church"))
	private Church church;

	@Column(name = "name", nullable = false, length = 200)
	private String name;

}
