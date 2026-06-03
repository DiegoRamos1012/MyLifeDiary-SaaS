package com.diegoramos.mylifediary.modules.addiction.dto.response;

import com.diegoramos.mylifediary.modules.addiction.domain.entity.Addiction;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Representação de saída de uma dependência.
 */
public record AddictionResponseDTO(
        UUID id,
        UUID userId,
        String title,
        String description,
        String category,
        LocalDate startDate,
        Instant createdAt
) {
    public static AddictionResponseDTO from(Addiction addiction) {
        return new AddictionResponseDTO(
                addiction.getId(),
                addiction.getUser().getId(),
                addiction.getTitle(),
                addiction.getDescription(),
                addiction.getAddictionCategory().name(),
                addiction.getStartDate(),
                addiction.getCreatedAt()
        );
    }
}

