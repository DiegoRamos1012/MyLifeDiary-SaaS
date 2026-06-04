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
 * Testes unitários do job responsável pelo hard delete de usuários INACTIVE.
 */
class UserHardDeletionJobTest {

    /**
     * Valida que o hard delete é executado com o threshold de 37 dias desde a solicitação.
     */
    @Test
    void hardDeleteInactiveUsersShouldDeleteAfter37DaysFromDeletionRequest() {
        UserRepository userRepository = mock(UserRepository.class);
        Instant now = Instant.parse("2026-04-20T00:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        UserHardDeletionJob userHardDeletionJob = new UserHardDeletionJob(userRepository, clock);

        userHardDeletionJob.hardDeleteInactiveUsers();

        Instant threshold = now.minus(37, ChronoUnit.DAYS);
        verify(userRepository).hardDeleteInactiveUsersBefore(UserStatus.INACTIVE, threshold);
        verifyNoMoreInteractions(userRepository);
    }
}

