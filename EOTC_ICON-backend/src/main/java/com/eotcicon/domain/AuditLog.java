package com.eotcicon.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

// An administrative action log entry.
@Getter
@Setter
@Entity
@Table(name = "audit_log")
public class AuditLog extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_user_id", foreignKey = @ForeignKey(name = "fk_audit_log_actor"))
	private User actor;

	@Column(name = "action", nullable = false, length = 80)
	private String action;

	@Column(name = "entity_type", nullable = false, length = 60)
	private String entityType;

	@Column(name = "entity_id")
	private UUID entityId;

	@Column(name = "detail")
	private String detail;

	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	@Column(name = "user_agent", length = 300)
	private String userAgent;

}
