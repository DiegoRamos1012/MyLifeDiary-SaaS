package com.diegoramos.mylifediary.modules.habit.domain.entity;
import com.diegoramos.mylifediary.common.exception.DomainException;
import com.diegoramos.mylifediary.modules.habit.domain.enums.HabitCategory;
import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
class HabitLogTest {
    private static Habit createHabit() {
        User user = User.create("john@example.com", "hash", "John Doe", LocalDate.of(1990, 1, 1));
        return Habit.create(user, "Estudar", null, HabitCategory.STUDY, null, LocalDate.of(2026, 5, 1));
    }
    @Test
    void createShouldNormalizeNote() {
        HabitLog log = HabitLog.create(createHabit(), LocalDate.of(2026, 5, 10), true, "  conclui tudo  ");
        assertTrue(log.isCompleted());
        assertEquals("conclui tudo", log.getNote());
    }
    @Test
    void createShouldThrowWhenCompletedIsNull() {
        DomainException exception = assertThrows(
                DomainException.class,
                () -> HabitLog.create(createHabit(), LocalDate.of(2026, 5, 10), null, null)
        );
        assertEquals("O status de conclusão não pode estar vazio", exception.getMessage());
    }
    @Test
    void markShouldUpdateStateAndClearBlankNote() {
        HabitLog log = HabitLog.create(createHabit(), LocalDate.of(2026, 5, 10), true, "ok");
        log.mark(false, "   ");
        assertEquals(false, log.isCompleted());
        assertNull(log.getNote());
    }
}
