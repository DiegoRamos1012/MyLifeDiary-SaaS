package com.diegoramos.mylifediary.modules.habit.service;
import com.diegoramos.mylifediary.common.result.Result;
import com.diegoramos.mylifediary.modules.habit.domain.entity.Habit;
import com.diegoramos.mylifediary.modules.habit.domain.entity.HabitLog;
import com.diegoramos.mylifediary.modules.habit.domain.enums.HabitCategory;
import com.diegoramos.mylifediary.modules.habit.dto.request.CreateHabitRequest;
import com.diegoramos.mylifediary.modules.habit.dto.request.MarkHabitDayRequest;
import com.diegoramos.mylifediary.modules.habit.dto.response.HabitLogResponseDTO;
import com.diegoramos.mylifediary.modules.habit.dto.response.HabitResponseDTO;
import com.diegoramos.mylifediary.modules.habit.dto.response.HabitStreakResponseDTO;
import com.diegoramos.mylifediary.modules.habit.repository.HabitLogRepository;
import com.diegoramos.mylifediary.modules.habit.repository.HabitRepository;
import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import com.diegoramos.mylifediary.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HabitServiceTest {
    @Mock
    private HabitRepository habitRepository;
    @Mock
    private HabitLogRepository habitLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private Clock clock;
    @InjectMocks
    private HabitService habitService;
    private UUID habitId;
    private User user;
    private Habit habit;
    @BeforeEach
    void setUp() {
        habitId = UUID.randomUUID();
        user = User.create("john@example.com", "hash", "John Doe", LocalDate.of(1990, 1, 1));
        habit = Habit.create(user, "Estudar", null, HabitCategory.STUDY, null, LocalDate.of(2026, 5, 1));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(clock.instant()).thenReturn(Instant.parse("2026-05-16T00:00:00Z"));
    }
    @Test
    void createHabitShouldFailWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        Result<HabitResponseDTO> result = habitService.createHabit(
                userId,
                new CreateHabitRequest("Ler", null, HabitCategory.STUDY, null, LocalDate.of(2026, 5, 1))
        );
        assertTrue(result.isFailure());
        assertEquals("HABIT_USER_NOT_FOUND", result.getError().code());
    }
    @Test
    void createHabitShouldPersistWhenInputIsValid() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(habitRepository.save(any(Habit.class))).thenAnswer(inv -> inv.getArgument(0));
        Result<HabitResponseDTO> result = habitService.createHabit(
                userId,
                new CreateHabitRequest("  Ler diario  ", "  10 paginas ", HabitCategory.STUDY, 1, LocalDate.of(2026, 5, 1))
        );
        assertTrue(result.isSuccess());
        assertEquals("Ler diario", result.getValue().title());
        verify(habitRepository).save(any(Habit.class));
    }
    @Test
    void markHabitDayShouldCreateLogWhenNotExists() {
        when(habitRepository.findById(habitId)).thenReturn(Optional.of(habit));
        when(habitLogRepository.findByHabitIdAndDate(habitId, LocalDate.of(2026, 5, 16))).thenReturn(Optional.empty());
        when(habitLogRepository.save(any(HabitLog.class))).thenAnswer(inv -> inv.getArgument(0));
        Result<HabitLogResponseDTO> result = habitService.markHabitDay(
                habitId,
                new MarkHabitDayRequest(LocalDate.of(2026, 5, 16), true, "feito")
        );
        assertTrue(result.isSuccess());
        assertTrue(result.getValue().completed());
    }
    @Test
    void markHabitDayShouldUpdateExistingLog() {
        HabitLog existing = HabitLog.create(habit, LocalDate.of(2026, 5, 16), false, "nao");
        when(habitRepository.findById(habitId)).thenReturn(Optional.of(habit));
        when(habitLogRepository.findByHabitIdAndDate(habitId, LocalDate.of(2026, 5, 16))).thenReturn(Optional.of(existing));
        when(habitLogRepository.save(existing)).thenReturn(existing);
        Result<HabitLogResponseDTO> result = habitService.markHabitDay(
                habitId,
                new MarkHabitDayRequest(LocalDate.of(2026, 5, 16), true, "ok")
        );
        assertTrue(result.isSuccess());
        assertTrue(existing.isCompleted());
        assertEquals("ok", existing.getNote());
    }
    @Test
    void markHabitDayShouldFailWhenDateBeforeStartDate() {
        when(habitRepository.findById(habitId)).thenReturn(Optional.of(habit));
        Result<HabitLogResponseDTO> result = habitService.markHabitDay(
                habitId,
                new MarkHabitDayRequest(LocalDate.of(2026, 4, 30), true, null)
        );
        assertTrue(result.isFailure());
        assertEquals("HABIT_LOG_BEFORE_START_DATE", result.getError().code());
    }
    @Test
    void getHabitLogsShouldFailWhenRangeIsInvalid() {
        when(habitRepository.existsById(habitId)).thenReturn(true);
        Result<List<HabitLogResponseDTO>> result = habitService.getHabitLogs(
                habitId,
                LocalDate.of(2026, 5, 20),
                LocalDate.of(2026, 5, 10)
        );
        assertTrue(result.isFailure());
        assertEquals("HABIT_LOG_INVALID_RANGE", result.getError().code());
    }
    @Test
    void getHabitStreakShouldCountConsecutiveCompletedDays() {
        when(habitRepository.existsById(habitId)).thenReturn(true);
        when(habitLogRepository.findByHabitIdOrderByDateDesc(habitId)).thenReturn(List.of(
                HabitLog.create(habit, LocalDate.of(2026, 5, 16), true, null),
                HabitLog.create(habit, LocalDate.of(2026, 5, 15), true, null),
                HabitLog.create(habit, LocalDate.of(2026, 5, 14), true, null),
                HabitLog.create(habit, LocalDate.of(2026, 5, 13), false, null)
        ));
        Result<HabitStreakResponseDTO> result = habitService.getHabitStreak(habitId);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getValue().currentStreak());
    }
    @Test
    void getHabitStreakShouldReturnZeroWhenTodayHasNoLog() {
        when(habitRepository.existsById(habitId)).thenReturn(true);
        when(habitLogRepository.findByHabitIdOrderByDateDesc(habitId)).thenReturn(List.of(
                HabitLog.create(habit, LocalDate.of(2026, 5, 15), true, null)
        ));
        Result<HabitStreakResponseDTO> result = habitService.getHabitStreak(habitId);
        assertTrue(result.isSuccess());
        assertEquals(0, result.getValue().currentStreak());
    }
}
