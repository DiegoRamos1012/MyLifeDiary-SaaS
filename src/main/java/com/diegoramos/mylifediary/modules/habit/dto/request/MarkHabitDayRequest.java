package com.diegoramos.mylifediary.modules.habit.dto.request;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
/**
 * Payload usado para registrar ou atualizar o log diário de um hábito.
 *
 * @param date      data do registro
 * @param completed status de conclusão
 * @param note      observação opcional
 */
public record MarkHabitDayRequest(
        @NotNull(message = "A data do registro é obrigatória")
        LocalDate date,
        @NotNull(message = "O status de conclusão é obrigatório")
        Boolean completed,
        @Size(max = 500, message = "A nota deve ter no máximo 500 caracteres")
        String note
) {
}
