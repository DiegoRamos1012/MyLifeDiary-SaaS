package com.diegoramos.mylifediary.modules.auth.service;

import com.diegoramos.mylifediary.common.result.Result;
import com.diegoramos.mylifediary.config.jwt.JwtProperties;
import com.diegoramos.mylifediary.config.jwt.JwtService;
import com.diegoramos.mylifediary.modules.auth.domain.entity.RefreshToken;
import com.diegoramos.mylifediary.modules.auth.dto.request.LoginRequest;
import com.diegoramos.mylifediary.modules.auth.dto.request.RefreshRequest;
import com.diegoramos.mylifediary.modules.auth.dto.response.AuthResponse;
import com.diegoramos.mylifediary.modules.auth.repository.RefreshTokenRepository;
import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import com.diegoramos.mylifediary.modules.user.domain.enums.UserRole;
import com.diegoramos.mylifediary.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-15T00:00:00Z");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private Clock clock;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    @InjectMocks
    private AuthService authService;

    private static void setId(User user) throws Exception {
        Field field = User.class.getSuperclass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, AuthServiceTest.USER_ID);
    }

    private static void setRole(User user) throws Exception {
        Field field = User.class.getDeclaredField("role");
        field.setAccessible(true);
        field.set(user, UserRole.USER);
    }

    private static User buildActiveUser() throws Exception {
        User user = User.create("john@example.com", "hash", "John", LocalDate.of(1990, 1, 1));
        setId(user);
        setRole(user);
        return user;
    }

    private static RefreshToken buildToken(String tokenValue, Instant expiresAt, boolean revoked) {
        RefreshToken token = RefreshToken.create(USER_ID, tokenValue, expiresAt);
        if (revoked) {
            token.revoke();
        }
        return token;
    }

    @BeforeEach
    void setupClock() {
        lenient().when(clock.instant()).thenReturn(NOW);
        lenient().when(jwtProperties.getRefreshTokenExpirationDays()).thenReturn(7L);
    }

    @Test
    void authenticate_userNotFound_returnsInvalidCredentials() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        Result<AuthResponse> result = authService.authenticate(new LoginRequest("missing@example.com", "secret"));

        assertTrue(result.isFailure());
        assertEquals("AUTH_INVALID_CREDENTIALS", result.getError().code());
        assertEquals("Credenciais inválidas", result.getError().message());
    }

    @Test
    void authenticate_wrongPassword_returnsInvalidCredentials() {
        User user = User.create("john@example.com", "hash", "John", LocalDate.of(1990, 1, 1));
        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        Result<AuthResponse> result = authService.authenticate(new LoginRequest("john@example.com", "wrong"));

        assertTrue(result.isFailure());
        assertEquals("AUTH_INVALID_CREDENTIALS", result.getError().code());
        assertEquals("Credenciais inválidas", result.getError().message());
    }

    @Test
    void refresh_tokenNotFound_returnsFailure() {
        when(refreshTokenRepository.findByToken("missing-refresh")).thenReturn(Optional.empty());

        Result<AuthResponse> result = authService.refresh(new RefreshRequest("missing-refresh"));

        assertTrue(result.isFailure());
        assertEquals("AUTH_REFRESH_TOKEN_NOT_FOUND", result.getError().code());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_tokenRevoked_returnsFailure() {
        RefreshToken token = buildToken("tok", NOW.plusSeconds(3600), true);
        when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));

        Result<AuthResponse> result = authService.refresh(new RefreshRequest("tok"));

        assertTrue(result.isFailure());
        assertEquals("AUTH_REFRESH_TOKEN_REVOKED", result.getError().code());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_tokenExpired_returnsFailure() {
        RefreshToken token = buildToken("tok", NOW.minusSeconds(1), false);
        when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));

        Result<AuthResponse> result = authService.refresh(new RefreshRequest("tok"));

        assertTrue(result.isFailure());
        assertEquals("AUTH_REFRESH_TOKEN_EXPIRED", result.getError().code());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_validToken_rotatesRefreshTokenAndReturnsNewTokens() throws Exception {
        User user = buildActiveUser();
        RefreshToken token = buildToken("old-refresh", NOW.plusSeconds(3600), false);

        when(refreshTokenRepository.findByToken("old-refresh")).thenReturn(Optional.of(token));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(jwtService.generateToken("john@example.com", "USER")).thenReturn("new-access-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        Result<AuthResponse> result = authService.refresh(new RefreshRequest("old-refresh"));

        assertTrue(result.isSuccess());
        assertEquals("new-access-token", result.getValue().accessToken());
        assertEquals("Bearer", result.getValue().tokenType());
        assertEquals(3600L, result.getValue().expiresIn());
        assertNotEquals("old-refresh", result.getValue().refreshToken());

        verify(refreshTokenRepository, times(2)).save(refreshTokenCaptor.capture());
        assertEquals(2, refreshTokenCaptor.getAllValues().size());

        RefreshToken revokedToken = refreshTokenCaptor.getAllValues().get(0);
        RefreshToken newToken = refreshTokenCaptor.getAllValues().get(1);

        assertTrue(revokedToken.isRevoked());
        assertEquals("old-refresh", revokedToken.getToken());
        assertEquals(USER_ID, revokedToken.getUserId());

        assertEquals(USER_ID, newToken.getUserId());
        assertEquals(result.getValue().refreshToken(), newToken.getToken());
        assertEquals(NOW.plusSeconds(7L * 24L * 60L * 60L), newToken.getExpiresAt());
        assertFalse(newToken.isRevoked());
    }

    @Test
    void logout_tokenNotFound_returnsFailure() {
        when(refreshTokenRepository.findByToken("missing-refresh")).thenReturn(Optional.empty());

        Result<Void> result = authService.logout(new RefreshRequest("missing-refresh"));

        assertTrue(result.isFailure());
        assertEquals("AUTH_REFRESH_TOKEN_NOT_FOUND", result.getError().code());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logout_validToken_revokesTokenAndSucceeds() {
        RefreshToken token = buildToken("tok", NOW.plusSeconds(3600), false);
        when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));

        Result<Void> result = authService.logout(new RefreshRequest("tok"));

        assertTrue(result.isSuccess());
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        assertTrue(refreshTokenCaptor.getValue().isRevoked());
    }

    @Test
    void logout_alreadyRevokedToken_stillSucceeds() {
        RefreshToken token = buildToken("tok", NOW.plusSeconds(3600), true);
        when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));

        Result<Void> result = authService.logout(new RefreshRequest("tok"));

        assertTrue(result.isSuccess());
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        assertTrue(refreshTokenCaptor.getValue().isRevoked());
    }
}

