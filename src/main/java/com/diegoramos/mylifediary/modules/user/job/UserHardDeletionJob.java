package com.diegoramos.mylifediary.modules.user.job;

import com.diegoramos.mylifediary.modules.user.domain.enums.UserStatus;
import com.diegoramos.mylifediary.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class UserHardDeletionJob {

    private static final long PENDING_DELETION_GRACE_DAYS = 30;
    private static final long INACTIVE_RETENTION_DAYS = 7;

    private final UserRepository userRepository;
    private final Clock clock;

    /**
     * Executa todos os dias às 00:30 (UTC).
     * Remove definitivamente usuários INACTIVE quando já passaram 30 + 7 dias
     * desde o pedido de exclusão.
     */
    @Transactional
    @Scheduled(cron = "0 30 0 * * *", zone = "UTC")
    public void hardDeleteInactiveUsers() {
        long totalRetentionDays = PENDING_DELETION_GRACE_DAYS + INACTIVE_RETENTION_DAYS;
        Instant threshold = clock.instant().minus(totalRetentionDays, ChronoUnit.DAYS);

        userRepository.hardDeleteInactiveUsersBefore(UserStatus.INACTIVE, threshold);
    }
}

