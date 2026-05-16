package com.diegoramos.mylifediary.modules.auth.domain.entity;

import com.diegoramos.mylifediary.common.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenTest {

    private static RefreshToken createValidToken() {
        return RefreshToken.create(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "refresh-token-value",
                Instant.parse("2099-01-01T00:00:00Z")
        );
    }

    @Test
    void createShouldReturnTokenWithCorrectFields() {
        RefreshToken token = createValidToken();

        assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), token.getUserId());
        assertEquals("refresh-token-value", token.getToken());
        assertEquals(Instant.parse("2099-01-01T00:00:00Z"), token.getExpiresAt());
        assertFalse(token.isRevoked());
    }

    @Test
    void createShouldThrowWhenUserIdIsNull() {
        DomainException exception = assertThrows(DomainException.class,
                () -> RefreshToken.create(null, "tok", Instant.parse("2099-01-01T00:00:00Z")));

        assertEquals("Erro: userId não pode estar vazio", exception.getMessage());
    }

    @Test
    void createShouldThrowWhenTokenIsBlank() {
        DomainException exception = assertThrows(DomainException.class,
                () -> RefreshToken.create(UUID.randomUUID(), " ", Instant.parse("2099-01-01T00:00:00Z")));

        assertEquals("Erro: refresh token inválido", exception.getMessage());
    }

    @Test
    void createShouldThrowWhenExpiresAtIsNull() {
        DomainException exception = assertThrows(DomainException.class,
                () -> RefreshToken.create(UUID.randomUUID(), "tok", null));

        assertEquals("Erro: data de expiração inválida", exception.getMessage());
    }

    @Test
    void isExpiredShouldReturnTrueWhenPastExpiration() {
        RefreshToken token = RefreshToken.create(
                UUID.randomUUID(),
                "tok",
                Instant.parse("2020-01-01T00:00:00Z")
        );

        assertTrue(token.isExpired(Instant.parse("2026-05-15T00:00:00Z")));
    }

    @Test
    void isExpiredShouldReturnFalseWhenBeforeExpiration() {
        RefreshToken token = RefreshToken.create(
                UUID.randomUUID(),
                "tok",
                Instant.parse("2099-01-01T00:00:00Z")
        );

        assertFalse(token.isExpired(Instant.parse("2026-05-15T00:00:00Z")));
    }

    @Test
    void isExpiredShouldReturnTrueWhenExactlyAtExpiration() {
        Instant now = Instant.parse("2026-05-15T00:00:00Z");
        RefreshToken token = RefreshToken.create(UUID.randomUUID(), "tok", now);

        assertTrue(token.isExpired(now));
    }

    @Test
    void revokeShouldSetRevokedToTrue() {
        RefreshToken token = createValidToken();

        token.revoke();

        assertTrue(token.isRevoked());
    }

    @Test
    void revokeShouldBeIdempotent() {
        RefreshToken token = createValidToken();

        token.revoke();
        token.revoke();

        assertTrue(token.isRevoked());
    }
}
