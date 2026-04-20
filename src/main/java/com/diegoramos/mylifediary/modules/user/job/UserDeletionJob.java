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
public class UserDeletionJob {

    private final UserRepository userRepository;
    private final Clock clock;

    /**
     * Executa todos os dias à meia-noite.
     * Move usuários que estão há mais de 30 dias em PENDING_DELETION para INACTIVE.
     */
    @Transactional
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void processPendingDeletionUsers() {
        Instant threshold = clock.instant().minus(30, ChronoUnit.DAYS);
        userRepository.markPendingDeletionUsersAsInactive(
                UserStatus.PENDING_DELETION,
                UserStatus.INACTIVE,
                threshold
        );
    }
}