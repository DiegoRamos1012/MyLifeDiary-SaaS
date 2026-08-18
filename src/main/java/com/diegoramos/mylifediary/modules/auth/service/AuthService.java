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
import com.diegoramos.mylifediary.modules.user.domain.enums.UserStatus;
import com.diegoramos.mylifediary.modules.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProperties,
                       Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    public Result<AuthResponse> authenticate(LoginRequest request) {
        Optional<User> maybeUser = userRepository.findByEmailIgnoreCase(request.email());

        // Generic error message to avoid user enumeration
        String genericError = "Credenciais inválidas";

        if (maybeUser.isEmpty()) {
            return Result.failure("AUTH_INVALID_CREDENTIALS", genericError);
        }

        User user = maybeUser.get();

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            return Result.failure("AUTH_INVALID_CREDENTIALS", genericError);
        }

        if (!user.isEmailVerified()) {
            return Result.failure("AUTH_EMAIL_NOT_VERIFIED", "E-mail não verificado. Verifique sua caixa de entrada");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            return Result.failure("AUTH_ACCOUNT_NOT_ACTIVE", "Conta não está ativa");
        }

        String refreshTokenValue = generateRefreshTokenValue();
        RefreshToken refreshToken = RefreshToken.create(user.getId(), refreshTokenValue, calculateRefreshTokenExpiration(clock.instant()));
        refreshTokenRepository.save(refreshToken);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), resolveRole(user));
        long expiresIn = jwtService.getExpirationSeconds();

        AuthResponse response = new AuthResponse(token, refreshTokenValue, "Bearer", expiresIn);
        return Result.success(response);
    }

    public Result<AuthResponse> refresh(RefreshRequest request) {
        Optional<RefreshToken> maybeToken = refreshTokenRepository.findByToken(request.refreshToken());
        if (maybeToken.isEmpty()) {
            return Result.failure("AUTH_REFRESH_TOKEN_NOT_FOUND", "Refresh token não encontrado");
        }

        RefreshToken refreshToken = maybeToken.get();
        if (refreshToken.isRevoked()) {
            return Result.failure("AUTH_REFRESH_TOKEN_REVOKED", "Refresh token revogado");
        }

        Instant now = clock.instant();
        if (refreshToken.isExpired(now)) {
            return Result.failure("AUTH_REFRESH_TOKEN_EXPIRED", "Refresh token expirado");
        }

        Optional<User> maybeUser = userRepository.findById(refreshToken.getUserId());
        if (maybeUser.isEmpty()) {
            return Result.failure("AUTH_REFRESH_TOKEN_NOT_FOUND", "Refresh token não encontrado");
        }

        User user = maybeUser.get();
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        RefreshToken newRefreshToken = RefreshToken.create(user.getId(), generateRefreshTokenValue(), calculateRefreshTokenExpiration(now));
        refreshTokenRepository.save(newRefreshToken);

        String accessToken = jwtService.generateToken(user.getId(), user.getEmail(), resolveRole(user));
        AuthResponse response = new AuthResponse(accessToken, newRefreshToken.getToken(), "Bearer", jwtService.getExpirationSeconds());
        return Result.success(response);
    }

    public Result<Void> logout(RefreshRequest request) {
        Optional<RefreshToken> maybeToken = refreshTokenRepository.findByToken(request.refreshToken());
        if (maybeToken.isEmpty()) {
            return Result.failure("AUTH_REFRESH_TOKEN_NOT_FOUND", "Refresh token não encontrado");
        }

        RefreshToken refreshToken = maybeToken.get();
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        return Result.success(null);
    }

    private String resolveRole(User user) {
        return user.getRole() == null ? "USER" : user.getRole().name();
    }

    private Instant calculateRefreshTokenExpiration(Instant now) {
        return now.plus(Duration.ofDays(jwtProperties.getRefreshTokenExpirationDays()));
    }

    private String generateRefreshTokenValue() {
        return UUID.randomUUID().toString();
    }
}

