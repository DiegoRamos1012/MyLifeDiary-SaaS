package com.diegoramos.mylifediary.modules.journal.dto.response;

import com.diegoramos.mylifediary.modules.journal.domain.entity.JournalEntry;
import com.diegoramos.mylifediary.modules.journal.domain.enums.MoodTypes;

import java.time.Instant;
import java.util.UUID;

public record JournalEntryResponseDTO(
        UUID id,
        UUID journalId,
        String content,
        MoodTypes mood,
        Instant createdAt,
        Instant updatedAt
) {
    public static JournalEntryResponseDTO from(JournalEntry entry) {
        return new JournalEntryResponseDTO(
                entry.getId(),
                entry.getJournal().getId(),
                entry.getContent(),
                entry.getMood(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}

