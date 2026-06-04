package com.diegoramos.mylifediary.modules.habit.dto.request;
import com.diegoramos.mylifediary.modules.habit.domain.enums.HabitCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
/**
 * Payload de criação de hábito enviado pela API.
 *
 * @param title      título do hábito
 * @param description descrição opcional
 * @param category    categoria do hábito
 * @param goalDaily   meta diária opcional
 * @param startDate   data de início do hábito
 */
public record CreateHabitRequest(
        @NotBlank(message = "O título do hábito é obrigatório")
        String title,
        String description,
        @NotNull(message = "A categoria do hábito é obrigatória")
        HabitCategory category,
        @Positive(message = "A meta diária deve ser maior que zero")
        Integer goalDaily,
        @NotNull(message = "A data de início é obrigatória")
        LocalDate startDate
) {
}
