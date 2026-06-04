package com.diegoramos.mylifediary.modules.addiction.service;

import com.diegoramos.mylifediary.common.exception.DomainException;
import com.diegoramos.mylifediary.common.result.Result;
import com.diegoramos.mylifediary.modules.addiction.domain.entity.Addiction;
import com.diegoramos.mylifediary.modules.addiction.domain.entity.AddictionLog;
import com.diegoramos.mylifediary.modules.addiction.dto.request.CreateAddictionRequest;
import com.diegoramos.mylifediary.modules.addiction.dto.request.RegisterAddictionLogRequest;
import com.diegoramos.mylifediary.modules.addiction.dto.response.AddictionLogResponseDTO;
import com.diegoramos.mylifediary.modules.addiction.dto.response.AddictionResponseDTO;
import com.diegoramos.mylifediary.modules.addiction.dto.response.AddictionSobrietyStreakResponseDTO;
import com.diegoramos.mylifediary.modules.addiction.repository.AddictionLogRepository;
import com.diegoramos.mylifediary.modules.addiction.repository.AddictionRepository;
import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import com.diegoramos.mylifediary.modules.user.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
 * Serviço de aplicação do módulo de dependências.
 */
@Service
@Transactional
public class AddictionService {

    private final AddictionRepository addictionRepository;
    private final AddictionLogRepository addictionLogRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public AddictionService(AddictionRepository addictionRepository,
                            AddictionLogRepository addictionLogRepository,
                            UserRepository userRepository,
                            Clock clock) {
        this.addictionRepository = addictionRepository;
        this.addictionLogRepository = addictionLogRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public Result<AddictionResponseDTO> createAddiction(UUID userId, @NonNull CreateAddictionRequest request) {
        Optional<User> maybeUser = userRepository.findById(userId);
        if (maybeUser.isEmpty()) {
            return Result.failure("ADDICTION_USER_NOT_FOUND", "Usuário não encontrado");
        }

        try {
            Addiction addiction = Addiction.create(
                    maybeUser.get(),
                    request.title(),
                    request.description(),
                    request.category(),
                    request.startDate()
            );
            Addiction saved = addictionRepository.save(addiction);
            return Result.success(AddictionResponseDTO.from(saved));
        } catch (DomainException ex) {
            return Result.failure("ADDICTION_INVALID_INPUT", ex.getMessage());
        }
    }

    public Result<AddictionLogResponseDTO> registerAddictionLog(UUID addictionId, UUID userId, @NonNull RegisterAddictionLogRequest request) {
        Optional<Addiction> maybeAddiction = addictionRepository.findByIdAndUserId(addictionId, userId);
        if (maybeAddiction.isEmpty()) {
            return Result.failure("ADDICTION_NOT_FOUND", "Dependência não encontrada");
        }

        Addiction addiction = maybeAddiction.get();
        if (request.date().isBefore(addiction.getStartDate())) {
            return Result.failure("ADDICTION_LOG_BEFORE_START_DATE", "A data do registro não pode ser anterior ao início da dependência");
        }

        try {
            Optional<AddictionLog> maybeLog = addictionLogRepository.findByAddictionIdAndDate(addictionId, request.date());
            AddictionLog saved = maybeLog
                    .map(existing -> {
                        existing.mark(request.relapsed(), request.note());
                        return addictionLogRepository.save(existing);
                    })
                    .orElseGet(() -> addictionLogRepository.save(
                            AddictionLog.create(addiction, request.date(), request.relapsed(), request.note())
                    ));
            return Result.success(AddictionLogResponseDTO.from(saved));
        } catch (DomainException ex) {
            return Result.failure("ADDICTION_INVALID_INPUT", ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Result<List<AddictionLogResponseDTO>> getAddictionLogs(UUID addictionId, UUID userId, LocalDate fromDate, LocalDate toDate) {
        if (!addictionRepository.existsByIdAndUserId(addictionId, userId)) {
            return Result.failure("ADDICTION_NOT_FOUND", "Dependência não encontrada");
        }

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            return Result.failure("ADDICTION_LOG_INVALID_RANGE", "Intervalo de datas inválido");
        }

        boolean hasFrom = fromDate != null;
        boolean hasTo = toDate != null;

        List<AddictionLog> logs;
        if (hasFrom && hasTo)
            logs = addictionLogRepository.findByAddictionIdAndDateBetweenOrderByDateAsc(addictionId, fromDate, toDate);
        else if (hasFrom)
            logs = addictionLogRepository.findByAddictionIdAndDateGreaterThanEqualOrderByDateAsc(addictionId, fromDate);
        else if (hasTo)
            logs = addictionLogRepository.findByAddictionIdAndDateLessThanEqualOrderByDateAsc(addictionId, toDate);
        else logs = addictionLogRepository.findByAddictionIdOrderByDateAsc(addictionId);

        return Result.success(logs.stream().map(AddictionLogResponseDTO::from).toList());
    }

    @Transactional(readOnly = true)
    public Result<AddictionSobrietyStreakResponseDTO> getCurrentSobrietyStreak(UUID addictionId, UUID userId) {
        if (!addictionRepository.existsByIdAndUserId(addictionId, userId)) {
            return Result.failure("ADDICTION_NOT_FOUND", "Dependência não encontrada");
        }

        List<AddictionLog> logs = addictionLogRepository.findByAddictionIdOrderByDateDesc(addictionId);
        if (logs.isEmpty()) {
            return Result.success(new AddictionSobrietyStreakResponseDTO(addictionId, 0));
        }

        Map<LocalDate, AddictionLog> logsByDate = logs.stream()
                .collect(Collectors.toMap(AddictionLog::getDate, log -> log));

        int streak = 0;
        LocalDate cursor = LocalDate.now(clock);

        while (true) {
            Optional<AddictionLog> maybeLog = Optional.ofNullable(logsByDate.get(cursor));

            if (maybeLog.isEmpty() || maybeLog.get().isRelapsed()) {
                break;
            }

            streak++;
            cursor = cursor.minusDays(1);
        }

        return Result.success(new AddictionSobrietyStreakResponseDTO(addictionId, streak));
    }

    @Transactional(readOnly = true)
    public Result<Page<AddictionResponseDTO>> findAll(UUID userId, int page, int size) {
        if (userRepository.findById(userId).isEmpty()) {
            return Result.failure("ADDICTION_USER_NOT_FOUND", "Usuário não encontrado");
        }

        Pageable pageable = PageRequest.of(page, size);
        return Result.success(addictionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(AddictionResponseDTO::from));
    }

    /**
     * Convenience method to find addictions using the user's email (useful when
     * the authenticated principal provides email as the JWT subject).
     */
    @Transactional(readOnly = true)
    public Result<Page<AddictionResponseDTO>> findAllByEmail(String email, int page, int size) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(user -> findAll(user.getId(), page, size))
                .orElseGet(() -> Result.failure("ADDICTION_USER_NOT_FOUND", "Usuário não encontrado"));
    }
}




