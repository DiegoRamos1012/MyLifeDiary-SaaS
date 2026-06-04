package com.diegoramos.mylifediary.modules.addiction.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload para registrar recaída ou dia de sobriedade.
 */
public record RegisterAddictionLogRequest(
        @NotNull(message = "A data do registro é obrigatória")
        LocalDate date,
        @NotNull(message = "O status de recaída é obrigatório")
        Boolean relapsed,
        @Size(max = 500, message = "A nota deve ter no máximo 500 caracteres")
        String note
) {
}

