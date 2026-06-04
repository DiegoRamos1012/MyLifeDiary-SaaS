package com.diegoramos.mylifediary.modules.user.job;

import com.diegoramos.mylifediary.modules.user.domain.enums.UserStatus;
import com.diegoramos.mylifediary.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Testes unitários do job responsável por mover usuários de PENDING_DELETION para INACTIVE.
 */
class UserDeletionJobTest {

    /**
     * Valida que o job executa atualização em lote com o threshold de 30 dias.
     */
    @Test
    void processPendingDeletionUsersShouldMarkPendingUsersAsInactiveAfter30Days() {
        UserRepository userRepository = mock(UserRepository.class);
        Instant now = Instant.parse("2026-04-20T00:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        UserDeletionJob userDeletionJob = new UserDeletionJob(userRepository, clock);

        userDeletionJob.processPendingDeletionUsers();

        Instant threshold = now.minus(30, ChronoUnit.DAYS);
        verify(userRepository).markPendingDeletionUsersAsInactive(
                UserStatus.PENDING_DELETION,
                UserStatus.INACTIVE,
                threshold
        );
        verifyNoMoreInteractions(userRepository);
    }
}


