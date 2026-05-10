package com.diegoramos.mylifediary.modules.auth.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}

