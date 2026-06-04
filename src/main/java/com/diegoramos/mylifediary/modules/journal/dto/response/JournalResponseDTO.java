package com.diegoramos.mylifediary.modules.journal.dto.response;

import com.diegoramos.mylifediary.modules.journal.domain.entity.Journal;

import java.time.Instant;
import java.util.UUID;

public record JournalResponseDTO(
        UUID id,
        UUID userId,
        String title,
        boolean isLocked,
        Instant createdAt,
        Instant updatedAt
) {
    public static JournalResponseDTO from(Journal journal) {
        return new JournalResponseDTO(
                journal.getId(),
                journal.getUser().getId(),
                journal.getTitle(),
                journal.isLocked(),
                journal.getCreatedAt(),
                journal.getUpdatedAt()
        );
    }
}

