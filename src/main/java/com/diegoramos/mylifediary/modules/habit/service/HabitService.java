package com.diegoramos.mylifediary.modules.habit.service;

import com.diegoramos.mylifediary.common.exception.DomainException;
import com.diegoramos.mylifediary.common.result.Result;
import com.diegoramos.mylifediary.modules.habit.domain.entity.Habit;
import com.diegoramos.mylifediary.modules.habit.domain.entity.HabitLog;
import com.diegoramos.mylifediary.modules.habit.dto.request.CreateHabitRequest;
import com.diegoramos.mylifediary.modules.habit.dto.request.MarkHabitDayRequest;
import com.diegoramos.mylifediary.modules.habit.dto.response.HabitLogResponseDTO;
import com.diegoramos.mylifediary.modules.habit.dto.response.HabitResponseDTO;
import com.diegoramos.mylifediary.modules.habit.dto.response.HabitStreakResponseDTO;
import com.diegoramos.mylifediary.modules.habit.repository.HabitLogRepository;
import com.diegoramos.mylifediary.modules.habit.repository.HabitRepository;
import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import com.diegoramos.mylifediary.modules.user.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço de aplicação responsável pelos casos de uso do módulo de hábitos.
 *
 * <p>Centraliza criação de hábitos, marcação de dias, consulta de histórico e
 * cálculo de streak, mantendo as regras de negócio fora da camada web.</p>
 */
@Service
@Transactional
public class HabitService {
    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /**
     * Cria uma instância do serviço com as dependências necessárias.
     */
    public HabitService(HabitRepository habitRepository,
                        HabitLogRepository habitLogRepository,
                        UserRepository userRepository,
                        Clock clock) {
        this.habitRepository = habitRepository;
        this.habitLogRepository = habitLogRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /**
     * Cria um hábito para o usuário informado.
     */
    public Result<HabitResponseDTO> createHabit(UUID userId, @NonNull CreateHabitRequest request) {
        Optional<User> maybeUser = userRepository.findById(userId);
        if (maybeUser.isEmpty()) {
            return Result.failure("HABIT_USER_NOT_FOUND", "Usuário não encontrado");
        }

        try {
            Habit habit = Habit.create(
                    maybeUser.get(),
                    request.title(),
                    request.description(),
                    request.category(),
                    request.goalDaily(),
                    request.startDate()
            );

            Habit saved = habitRepository.save(habit);
            return Result.success(HabitResponseDTO.from(saved));
        } catch (DomainException ex) {
            return Result.failure("HABIT_INVALID_INPUT", ex.getMessage());
        }
    }

    /**
     * Cria ou atualiza o log diário de um hábito.
     */
    public Result<HabitLogResponseDTO> markHabitDay(
            UUID habitId,
            @NonNull MarkHabitDayRequest request
    ) {
        LocalDate today = LocalDate.now(clock);

        Optional<Habit> maybeHabit = habitRepository.findById(habitId);
        if (maybeHabit.isEmpty()) {
            return Result.failure("HABIT_NOT_FOUND", "Hábito não encontrado");
        }

        Habit habit = maybeHabit.get();

        if (request.date().isBefore(habit.getStartDate())) {
            return Result.failure(
                    "HABIT_LOG_BEFORE_START_DATE",
                    "Data do log não pode ser anterior ao início do hábito"
            );
        }

        if (request.date().isAfter(today)) {
            return Result.failure(
                    "HABIT_LOG_AFTER_TODAY",
                    "Data do log não pode ser depois da data atual"
            );
        }

        try {
            Optional<HabitLog> maybeLog =
                    habitLogRepository.findByHabitIdAndDate(habitId, request.date());

            HabitLog saved = maybeLog
                    .map(existing -> {
                        existing.mark(request.completed(), request.note());
                        return habitLogRepository.save(existing);
                    })
                    .orElseGet(() -> habitLogRepository.save(
                            HabitLog.create(
                                    habit,
                                    request.date(),
                                    request.completed(),
                                    request.note()
                            )
                    ));

            return Result.success(HabitLogResponseDTO.from(saved));
        } catch (DomainException ex) {
            return Result.failure("HABIT_INVALID_INPUT", ex.getMessage());
        }
    }

    /**
     * Busca o histórico de logs de um hábito, opcionalmente filtrado por intervalo.
     */
    @Transactional(readOnly = true)
    public Result<List<HabitLogResponseDTO>> getHabitLogs(
            UUID habitId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (!habitRepository.existsById(habitId)) {
            return Result.failure("HABIT_NOT_FOUND", "Hábito não encontrado");
        }

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            return Result.failure(
                    "HABIT_LOG_INVALID_RANGE",
                    "Intervalo de datas inválido"
            );
        }

        List<HabitLog> logs;

        if (fromDate != null && toDate != null) {
            logs = habitLogRepository
                    .findByHabitIdAndDateBetweenOrderByDateAsc(
                            habitId,
                            fromDate,
                            toDate
                    );
        } else if (fromDate != null) {
            logs = habitLogRepository
                    .findByHabitIdAndDateGreaterThanEqualOrderByDateAsc(
                            habitId,
                            fromDate
                    );
        } else if (toDate != null) {
            logs = habitLogRepository
                    .findByHabitIdAndDateLessThanEqualOrderByDateAsc(
                            habitId,
                            toDate
                    );
        } else {
            logs = habitLogRepository.findByHabitIdOrderByDateAsc(habitId);
        }

        return Result.success(
                logs.stream()
                        .map(HabitLogResponseDTO::from)
                        .toList()
        );
    }

    /**
     * Calcula a streak atual do hábito informado.
     */
    @Transactional(readOnly = true)
    public Result<HabitStreakResponseDTO> getHabitStreak(UUID habitId) {
        if (!habitRepository.existsById(habitId)) {
            return Result.failure("HABIT_NOT_FOUND", "Hábito não encontrado");
        }

        List<HabitLog> logs =
                habitLogRepository.findByHabitIdOrderByDateDesc(habitId);

        if (logs.isEmpty()) {
            return Result.success(
                    new HabitStreakResponseDTO(habitId, 0)
            );
        }

        Map<LocalDate, Boolean> statusByDate = logs.stream()
                .collect(Collectors.toMap(
                        HabitLog::getDate,
                        HabitLog::isCompleted
                ));

        int streak = 0;
        LocalDate today = LocalDate.now(clock);

        while (Boolean.TRUE.equals(statusByDate.get(today))) {
            streak++;
            today = today.minusDays(1);
        }

        return Result.success(
                new HabitStreakResponseDTO(habitId, streak)
        );
    }
}