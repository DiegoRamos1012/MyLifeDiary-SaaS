package com.diegoramos.mylifediary.modules.habit.repository;
import com.diegoramos.mylifediary.modules.habit.domain.entity.HabitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
/**
 * Repositório JPA da entidade {@link com.diegoramos.mylifediary.modules.habit.domain.entity.HabitLog}.
 *
 * <p>Além das operações padrão, fornece consultas por hábito e data para
 * suportar o fluxo de histórico, upsert diário e cálculo de streak.</p>
 */
@Repository
public interface HabitLogRepository extends JpaRepository<HabitLog, UUID> {
    Optional<HabitLog> findByHabitIdAndDate(UUID habitId, LocalDate date);
    List<HabitLog> findByHabitIdOrderByDateAsc(UUID habitId);
    List<HabitLog> findByHabitIdOrderByDateDesc(UUID habitId);
    List<HabitLog> findByHabitIdAndDateBetweenOrderByDateAsc(UUID habitId, LocalDate fromDate, LocalDate toDate);
    List<HabitLog> findByHabitIdAndDateGreaterThanEqualOrderByDateAsc(UUID habitId, LocalDate fromDate);
    List<HabitLog> findByHabitIdAndDateLessThanEqualOrderByDateAsc(UUID habitId, LocalDate toDate);
}
