package com.diegoramos.mylifediary.modules.journal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateJournalRequest(
        @NotBlank(message = "O título do diário é obrigatório")
        @Size(min = 1, max = 80, message = "O título deve ter entre 1 e 80 caracteres")
        String title,

        boolean isLocked
) {
}

