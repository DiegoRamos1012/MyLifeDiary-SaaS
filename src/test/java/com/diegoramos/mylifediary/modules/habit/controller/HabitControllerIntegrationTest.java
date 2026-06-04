package com.diegoramos.mylifediary.modules.habit.controller;
import com.diegoramos.mylifediary.common.result.Result;
import com.diegoramos.mylifediary.modules.habit.domain.enums.HabitCategory;
import com.diegoramos.mylifediary.modules.habit.dto.request.CreateHabitRequest;
import com.diegoramos.mylifediary.modules.habit.dto.request.MarkHabitDayRequest;
import com.diegoramos.mylifediary.modules.habit.dto.response.HabitLogResponseDTO;
import com.diegoramos.mylifediary.modules.habit.dto.response.HabitResponseDTO;
import com.diegoramos.mylifediary.modules.habit.dto.response.HabitStreakResponseDTO;
import com.diegoramos.mylifediary.modules.habit.service.HabitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class HabitControllerIntegrationTest {
    @Mock
    private HabitService habitService;
    @InjectMocks
    private HabitController habitController;
    @Test
    void createHabitShouldReturnCreated() {
        UUID userId = UUID.randomUUID();
        CreateHabitRequest request = new CreateHabitRequest("Ler", null, HabitCategory.STUDY, null, LocalDate.of(2026, 5, 1));
        HabitResponseDTO response = new HabitResponseDTO(null, null, "Ler", null, "STUDY", null, LocalDate.of(2026, 5, 1), null);
        when(habitService.createHabit(userId, request)).thenReturn(Result.success(response));
        ResponseEntity<?> entity = habitController.createHabit(userId, request);
        assertEquals(HttpStatus.CREATED, entity.getStatusCode());
    }
    @Test
    void markHabitDayShouldReturnOk() {
        UUID habitId = UUID.randomUUID();
        MarkHabitDayRequest request = new MarkHabitDayRequest(LocalDate.of(2026, 5, 16), true, "ok");
        HabitLogResponseDTO response = new HabitLogResponseDTO(null, null, LocalDate.of(2026, 5, 16), true, "ok", null);
        when(habitService.markHabitDay(habitId, request)).thenReturn(Result.success(response));
        ResponseEntity<?> entity = habitController.markHabitDay(habitId, request);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
    }
    @Test
    void getHabitStreakShouldReturnOk() {
        UUID habitId = UUID.randomUUID();
        HabitStreakResponseDTO response = new HabitStreakResponseDTO(habitId, 4);
        when(habitService.getHabitStreak(habitId)).thenReturn(Result.success(response));
        ResponseEntity<?> entity = habitController.getHabitStreak(habitId);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
    }
}
