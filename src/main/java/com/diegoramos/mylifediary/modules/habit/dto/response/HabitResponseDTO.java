package com.diegoramos.mylifediary.modules.habit.dto.response;
import com.diegoramos.mylifediary.modules.habit.domain.entity.Habit;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
/**
 * Representação de saída de um hábito para a API.
 *
 * @param id          identificador do hábito
 * @param userId      identificador do usuário dono do hábito
 * @param title       título do hábito
 * @param description descrição do hábito
 * @param category    categoria do hábito
 * @param goalDaily   meta diária opcional
 * @param startDate   data de início
 * @param createdAt   instante de criação
 */
public record HabitResponseDTO(
        UUID id,
        UUID userId,
        String title,
        String description,
        String category,
        Integer goalDaily,
        LocalDate startDate,
        Instant createdAt
) {
    public static HabitResponseDTO from(Habit habit) {
        return new HabitResponseDTO(
                habit.getId(),
                habit.getUser().getId(),
                habit.getTitle(),
                habit.getDescription(),
                habit.getHabitCategory().name(),
                habit.getGoalDaily(),
                habit.getStartDate(),
                habit.getCreatedAt()
        );
    }
}
