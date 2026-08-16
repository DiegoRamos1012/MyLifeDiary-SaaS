package com.diegoramos.mylifediary.modules.user.job;

import com.diegoramos.mylifediary.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.mockito.Mockito.*;

class UserUnverifiedDeleteJobTest {

    @Test
    void shouldDeleteUnverifiedUsersAfter7Days() {
        UserRepository userRepository = mock(UserRepository.class);
        Instant now = Instant.parse("2026-04-20T00:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        UserUnverifiedDeletionJob userUnverifiedDeletionJob = new UserUnverifiedDeletionJob(userRepository, clock);

        userUnverifiedDeletionJob.deleteUnverifiedUsers();

        Instant threshold = now.minus(7, ChronoUnit.DAYS);
        verify(userRepository).hardDeleteUnverifiedUsers(threshold);
        verifyNoMoreInteractions(userRepository);
    }
}

