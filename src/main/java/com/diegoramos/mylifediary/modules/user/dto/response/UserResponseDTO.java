package com.diegoramos.mylifediary.modules.user.dto.response;

import com.diegoramos.mylifediary.modules.user.domain.entity.User;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String email,
        String fullName,
        LocalDate birthDate,
        String status,
        String role,
        boolean emailVerified,
        Instant createdAt
) {
    public static UserResponseDTO from(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getBirthDate(),
                user.getStatus().name(),
                user.getRole() == null ? null : user.getRole().name(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }
}
