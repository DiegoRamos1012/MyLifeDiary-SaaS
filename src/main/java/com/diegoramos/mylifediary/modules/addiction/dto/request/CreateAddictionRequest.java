package com.diegoramos.mylifediary.modules.addiction.dto.request;

import com.diegoramos.mylifediary.modules.addiction.domain.enums.AddictionCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Payload de criação de uma dependência.
 */
public record CreateAddictionRequest(
        @NotBlank(message = "O título da dependência é obrigatório")
        String title,
        String description,
        @NotNull(message = "A categoria da dependência é obrigatória")
        AddictionCategory category,
        @NotNull(message = "A data de início é obrigatória")
        LocalDate startDate
) {
}

