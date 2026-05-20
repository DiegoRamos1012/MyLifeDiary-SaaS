package com.diegoramos.mylifediary.modules.habit.dto.response;
import com.diegoramos.mylifediary.modules.habit.domain.entity.HabitLog;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
/**
 * Representação de saída de um log diário de hábito.
 *
 * @param id        identificador do log
 * @param habitId   identificador do hábito
 * @param date      data do registro
 * @param completed indica se o hábito foi concluído
 * @param note      observação registrada
 * @param createdAt instante de criação
 */
public record HabitLogResponseDTO(
        UUID id,
        UUID habitId,
        LocalDate date,
        boolean completed,
        String note,
        Instant createdAt
) {
    public static HabitLogResponseDTO from(HabitLog log) {
        return new HabitLogResponseDTO(
                log.getId(),
                log.getHabit().getId(),
                log.getDate(),
                log.isCompleted(),
                log.getNote(),
                log.getCreatedAt()
        );
    }
}
