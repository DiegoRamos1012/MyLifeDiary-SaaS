package com.diegoramos.mylifediary.modules.addiction.repository;

import com.diegoramos.mylifediary.modules.addiction.domain.entity.AddictionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA de {@link AddictionLog}.
 */
@Repository
public interface AddictionLogRepository extends JpaRepository<AddictionLog, UUID> {
    Optional<AddictionLog> findByAddictionIdAndDate(UUID addictionId, LocalDate date);
    List<AddictionLog> findByAddictionIdOrderByDateAsc(UUID addictionId);
    List<AddictionLog> findByAddictionIdOrderByDateDesc(UUID addictionId);
    List<AddictionLog> findByAddictionIdAndDateBetweenOrderByDateAsc(UUID addictionId, LocalDate fromDate, LocalDate toDate);
    List<AddictionLog> findByAddictionIdAndDateGreaterThanEqualOrderByDateAsc(UUID addictionId, LocalDate fromDate);
    List<AddictionLog> findByAddictionIdAndDateLessThanEqualOrderByDateAsc(UUID addictionId, LocalDate toDate);
}

