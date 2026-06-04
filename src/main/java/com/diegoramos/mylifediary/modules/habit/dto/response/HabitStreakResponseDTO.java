package com.diegoramos.mylifediary.modules.habit.dto.response;
import java.util.UUID;
/**
 * Representação de saída do cálculo da streak atual de um hábito.
 *
 * @param habitId       identificador do hábito
 * @param currentStreak  quantidade de dias consecutivos com conclusão
 */
public record HabitStreakResponseDTO(
        UUID habitId,
        int currentStreak
) {
}
