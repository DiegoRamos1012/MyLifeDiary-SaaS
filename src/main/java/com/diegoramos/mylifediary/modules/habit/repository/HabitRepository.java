package com.diegoramos.mylifediary.modules.habit.repository;

import com.diegoramos.mylifediary.modules.habit.domain.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositório JPA da entidade {@link com.diegoramos.mylifediary.modules.habit.domain.entity.Habit}.
 */
@Repository
public interface HabitRepository extends JpaRepository<Habit, UUID> {
}

