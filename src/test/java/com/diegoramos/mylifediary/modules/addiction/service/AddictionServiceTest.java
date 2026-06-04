package com.diegoramos.mylifediary.modules.addiction.service;

import com.diegoramos.mylifediary.common.result.Result;
import com.diegoramos.mylifediary.modules.addiction.domain.entity.Addiction;
import com.diegoramos.mylifediary.modules.addiction.domain.entity.AddictionLog;
import com.diegoramos.mylifediary.modules.addiction.domain.enums.AddictionCategory;
import com.diegoramos.mylifediary.modules.addiction.dto.request.CreateAddictionRequest;
import com.diegoramos.mylifediary.modules.addiction.dto.request.RegisterAddictionLogRequest;
import com.diegoramos.mylifediary.modules.addiction.dto.response.AddictionLogResponseDTO;
import com.diegoramos.mylifediary.modules.addiction.dto.response.AddictionResponseDTO;
import com.diegoramos.mylifediary.modules.addiction.dto.response.AddictionSobrietyStreakResponseDTO;
import com.diegoramos.mylifediary.modules.addiction.repository.AddictionLogRepository;
import com.diegoramos.mylifediary.modules.addiction.repository.AddictionRepository;
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
class AddictionServiceTest {

    @Mock
    private AddictionRepository addictionRepository;

    @Mock
    private AddictionLogRepository addictionLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private AddictionService addictionService;

    private UUID userId;
    private UUID addictionId;
    private User user;
    private Addiction addiction;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        addictionId = UUID.randomUUID();
        user = User.create("john@example.com", "hash", "John Doe", LocalDate.of(1990, 1, 1));
        addiction = Addiction.create(user, "Cigarro", null, AddictionCategory.SUBSTANCE, LocalDate.of(2026, 5, 1));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(clock.instant()).thenReturn(Instant.parse("2026-05-16T00:00:00Z"));
    }

    @Test
    void createAddictionShouldFailWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Result<AddictionResponseDTO> result = addictionService.createAddiction(
                userId,
                new CreateAddictionRequest("Cigarro", null, AddictionCategory.SUBSTANCE, LocalDate.of(2026, 5, 1))
        );

        assertTrue(result.isFailure());
        assertEquals("ADDICTION_USER_NOT_FOUND", result.getError().code());
    }

    @Test
    void createAddictionShouldPersistWhenInputIsValid() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(addictionRepository.save(any(Addiction.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<AddictionResponseDTO> result = addictionService.createAddiction(
                userId,
                new CreateAddictionRequest("Cigarro", "parar", AddictionCategory.SUBSTANCE, LocalDate.of(2026, 5, 1))
        );

        assertTrue(result.isSuccess());
        assertEquals("Cigarro", result.getValue().title());
        verify(addictionRepository).save(any(Addiction.class));
    }

    @Test
    void registerAddictionLogShouldCreateLogWhenNotExists() {
        when(addictionRepository.findByIdAndUserId(addictionId, userId)).thenReturn(Optional.of(addiction));
        when(addictionLogRepository.findByAddictionIdAndDate(addictionId, LocalDate.of(2026, 5, 16))).thenReturn(Optional.empty());
        when(addictionLogRepository.save(any(AddictionLog.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<AddictionLogResponseDTO> result = addictionService.registerAddictionLog(
                addictionId,
                userId,
                new RegisterAddictionLogRequest(LocalDate.of(2026, 5, 16), true, "recaída")
        );

        assertTrue(result.isSuccess());
        assertTrue(result.getValue().relapsed());
    }

    @Test
    void registerAddictionLogShouldFailWhenDateBeforeStartDate() {
        when(addictionRepository.findByIdAndUserId(addictionId, userId)).thenReturn(Optional.of(addiction));

        Result<AddictionLogResponseDTO> result = addictionService.registerAddictionLog(
                addictionId,
                userId,
                new RegisterAddictionLogRequest(LocalDate.of(2026, 4, 30), true, null)
        );

        assertTrue(result.isFailure());
        assertEquals("ADDICTION_LOG_BEFORE_START_DATE", result.getError().code());
    }

    @Test
    void getAddictionLogsShouldFailWhenRangeIsInvalid() {
        when(addictionRepository.existsByIdAndUserId(addictionId, userId)).thenReturn(true);

        Result<List<AddictionLogResponseDTO>> result = addictionService.getAddictionLogs(
                addictionId,
                userId,
                LocalDate.of(2026, 5, 20),
                LocalDate.of(2026, 5, 10)
        );

        assertTrue(result.isFailure());
        assertEquals("ADDICTION_LOG_INVALID_RANGE", result.getError().code());
    }

    @Test
    void getCurrentSobrietyStreakShouldCountConsecutiveSuccessDays() {
        when(addictionRepository.existsByIdAndUserId(addictionId, userId)).thenReturn(true);
        when(addictionLogRepository.findByAddictionIdOrderByDateDesc(addictionId)).thenReturn(List.of(
                AddictionLog.create(addiction, LocalDate.of(2026, 5, 16), false, null),
                AddictionLog.create(addiction, LocalDate.of(2026, 5, 15), false, null),
                AddictionLog.create(addiction, LocalDate.of(2026, 5, 14), false, null),
                AddictionLog.create(addiction, LocalDate.of(2026, 5, 13), true, null)
        ));

        Result<AddictionSobrietyStreakResponseDTO> result = addictionService.getCurrentSobrietyStreak(addictionId, userId);

        assertTrue(result.isSuccess());
        assertEquals(3, result.getValue().currentSobrietyStreak());
    }

    @Test
    void getCurrentSobrietyStreakShouldReturnZeroWhenTodayHasNoLog() {
        when(addictionRepository.existsByIdAndUserId(addictionId, userId)).thenReturn(true);
        when(addictionLogRepository.findByAddictionIdOrderByDateDesc(addictionId)).thenReturn(List.of(
                AddictionLog.create(addiction, LocalDate.of(2026, 5, 15), false, null)
        ));

        Result<AddictionSobrietyStreakResponseDTO> result = addictionService.getCurrentSobrietyStreak(addictionId, userId);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getValue().currentSobrietyStreak());
    }
}

