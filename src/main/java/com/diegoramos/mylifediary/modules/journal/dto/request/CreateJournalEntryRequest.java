package com.diegoramos.mylifediary.modules.journal.dto.request;

import com.diegoramos.mylifediary.modules.journal.domain.enums.MoodTypes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateJournalEntryRequest(
        @NotNull(message = "A data da entrada é obrigatória")
        LocalDate entryDate,

        @NotBlank(message = "O conteúdo da entrada é obrigatório")
        @Size(min = 1, max = 20000, message = "O conteúdo deve ter entre 1 e 20.000 caracteres")
        String content,

        @NotNull(message = "O humor é obrigatório")
        MoodTypes mood
) {
}

