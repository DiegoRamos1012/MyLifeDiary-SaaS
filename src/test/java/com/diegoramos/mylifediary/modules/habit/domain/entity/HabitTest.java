package com.diegoramos.mylifediary.modules.habit.domain.entity;
import com.diegoramos.mylifediary.common.exception.DomainException;
import com.diegoramos.mylifediary.modules.habit.domain.enums.HabitCategory;
import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
class HabitTest {
    private static User createUser() {
        return User.create("john@example.com", "hash", "John Doe", LocalDate.of(1990, 1, 1));
    }
    @Test
    void createShouldNormalizeFields() {
        Habit habit = Habit.create(
                createUser(),
                "  Ler   livro  ",
                "  20 paginas por dia  ",
                HabitCategory.STUDY,
                1,
                LocalDate.of(2026, 5, 1)
        );
        assertEquals("Ler livro", habit.getTitle());
        assertEquals("20 paginas por dia", habit.getDescription());
    }
    @Test
    void createShouldKeepDescriptionNullWhenBlank() {
        Habit habit = Habit.create(
                createUser(),
                "Meditar",
                "   ",
                HabitCategory.WELLNESS,
                null,
                LocalDate.of(2026, 5, 1)
        );
        assertNull(habit.getDescription());
    }
    @Test
    void createShouldThrowWhenGoalDailyIsInvalid() {
        DomainException exception = assertThrows(
                DomainException.class,
                () -> Habit.create(
                        createUser(),
                        "Exercicio",
                        null,
                        HabitCategory.EXERCISE,
                        0,
                        LocalDate.of(2026, 5, 1)
                )
        );
        assertEquals("Erro: meta diária inválida", exception.getMessage());
    }
}
