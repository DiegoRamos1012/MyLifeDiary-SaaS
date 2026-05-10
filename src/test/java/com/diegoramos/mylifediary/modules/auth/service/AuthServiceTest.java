package com.diegoramos.mylifediary.modules.auth.service;

import com.diegoramos.mylifediary.common.result.Result;
import com.diegoramos.mylifediary.config.security.JwtService;
import com.diegoramos.mylifediary.modules.auth.dto.request.LoginRequest;
import com.diegoramos.mylifediary.modules.auth.dto.response.AuthResponse;
import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import com.diegoramos.mylifediary.modules.user.domain.enums.UserStatus;
import com.diegoramos.mylifediary.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

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
    void authenticate_accountNotActive_returnsAccountNotActive() throws Exception {
        User user = User.create("john@example.com", "hash", "John", LocalDate.of(1990, 1, 1));
        setStatus(user, UserStatus.SUSPENDED);

        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);

        Result<AuthResponse> result = authService.authenticate(new LoginRequest("john@example.com", "secret"));

        assertTrue(result.isFailure());
        assertEquals("AUTH_ACCOUNT_NOT_ACTIVE", result.getError().code());
    }

    @Test
    void authenticate_success_returnsBearerToken() {
        User user = User.create("john@example.com", "hash", "John", LocalDate.of(1990, 1, 1));

        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(jwtService.generateToken("john@example.com", "USER")).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        Result<AuthResponse> result = authService.authenticate(new LoginRequest("john@example.com", "secret"));

        assertTrue(result.isSuccess());
        assertEquals("jwt-token", result.getValue().accessToken());
        assertEquals("Bearer", result.getValue().tokenType());
        assertEquals(3600L, result.getValue().expiresIn());

        verify(jwtService).generateToken("john@example.com", "USER");
    }

    private static void setStatus(User user, UserStatus status) throws Exception {
        Field field = User.class.getDeclaredField("status");
        field.setAccessible(true);
        field.set(user, status);
    }
}

