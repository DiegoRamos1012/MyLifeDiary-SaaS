package com.diegoramos.mylifediary.modules.auth.domain.entity;

import com.diegoramos.mylifediary.common.exception.DomainException;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

	@Id
	@Column(updatable = false, nullable = false)
	private UUID id;

	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@Column(nullable = false, unique = true, length = 255)
	private String token;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	private boolean revoked;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	private RefreshToken(UUID userId, String token, Instant expiresAt) {
		this.userId = userId;
		this.token = token;
		this.expiresAt = expiresAt;
		this.revoked = false;
	}

	public static RefreshToken create(UUID userId, String token, Instant expiresAt) {
		if (userId == null) {
			throw new DomainException("Erro: userId não pode estar vazio");
		}
		if (token == null || token.isBlank()) {
			throw new DomainException("Erro: refresh token inválido");
		}
		if (expiresAt == null) {
			throw new DomainException("Erro: data de expiração inválida");
		}

		return new RefreshToken(userId, token, expiresAt);
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UuidCreator.getTimeOrderedEpoch();
		}
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public boolean isExpired(Instant now) {
		if (now == null) {
			throw new IllegalArgumentException("now must not be null");
		}
		return !expiresAt.isAfter(now);
	}

	public void revoke() {
		this.revoked = true;
	}
}

