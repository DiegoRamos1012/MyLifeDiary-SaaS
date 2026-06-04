package com.diegoramos.mylifediary.modules.journal.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UnlockJournalRequest(
        @NotBlank(message = "A senha é obrigatória")
        String password
) {
}

