package com.diegoramos.mylifediary.modules.user.job;

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
public class UserUnverifiedDeletionJob {

    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")

    public void deleteUnverifiedUsers() {
        Instant threshold = clock.instant().minus(7, ChronoUnit.DAYS);
        userRepository.hardDeleteUnverifiedUsers(threshold);
    }
}
