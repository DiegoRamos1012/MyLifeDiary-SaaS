package com.diegoramos.mylifediary.modules.auth.service;

import com.diegoramos.mylifediary.common.result.Result;
import com.diegoramos.mylifediary.modules.auth.dto.request.LoginRequest;
import com.diegoramos.mylifediary.modules.auth.dto.response.AuthResponse;
import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import com.diegoramos.mylifediary.modules.user.domain.enums.UserStatus;
import com.diegoramos.mylifediary.modules.user.repository.UserRepository;
import com.diegoramos.mylifediary.config.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

        if (user.getStatus() != UserStatus.ACTIVE) {
            return Result.failure("AUTH_ACCOUNT_NOT_ACTIVE", "Conta não está ativa");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole() == null ? "USER" : user.getRole().name());
        long expiresIn = jwtService.getExpirationSeconds();

        AuthResponse response = new AuthResponse(token, "Bearer", expiresIn);
        return Result.success(response);
    }
}

