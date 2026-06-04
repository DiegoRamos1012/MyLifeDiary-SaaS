package com.diegoramos.mylifediary.modules.addiction.dto.response;

import com.diegoramos.mylifediary.modules.addiction.domain.entity.AddictionLog;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Representação de saída de um log diário de dependência.
 */
public record AddictionLogResponseDTO(
        UUID id,
        UUID addictionId,
        LocalDate date,
        boolean relapsed,
        String note,
        Instant createdAt
) {
    public static AddictionLogResponseDTO from(AddictionLog log) {
        return new AddictionLogResponseDTO(
                log.getId(),
                log.getAddiction().getId(),
                log.getDate(),
                log.isRelapsed(),
                log.getNote(),
                log.getCreatedAt()
        );
    }
}

