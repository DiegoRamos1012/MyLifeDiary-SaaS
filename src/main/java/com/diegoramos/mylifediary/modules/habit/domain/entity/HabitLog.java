package com.diegoramos.mylifediary.modules.habit.domain.entity;

import com.diegoramos.mylifediary.common.base.BaseEntity;
import com.diegoramos.mylifediary.common.util.DomainValidation;
import com.diegoramos.mylifediary.common.util.TextNormalizer;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "habit_logs",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"habit_id", "date"})
        }
)
/**
 * Entidade que representa o registro diário de execução de um hábito.
 *
 * <p>Um {@code HabitLog} é um evento real de execução, não o estado atual do
 * hábito. O modelo garante um registro por dia por hábito.</p>
 */
public class HabitLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "note", length = 500)
    private String note;

    /**
     * Construtor interno usado pela fábrica estática.
     */
    private HabitLog(Habit habit,
                     LocalDate date,
                     boolean completed,
                     String note) {

        this.habit = habit;
        this.date = date;
        this.completed = completed;
        this.note = note;
    }

    public static HabitLog create(Habit habit,
                                  LocalDate date,
                                  Boolean completed,
                                  String note) {

        DomainValidation.validateRequired(habit, "O hábito");
        DomainValidation.validateRequired(date, "A data");
        DomainValidation.validateRequired(completed, "O status de conclusão");

        return new HabitLog(
                habit,
                date,
                completed,
                normalizeNote(note)
        );
    }

    public void mark(Boolean completed, String note) {
        DomainValidation.validateRequired(completed, "O status de conclusão");
        this.completed = completed;
        this.note = normalizeNote(note);
        updateUpdatedAt();
    }

    /**
     * Normaliza a nota do log, removendo espaços laterais e convertendo vazio
     * em {@code null}.
     */
    private static String normalizeNote(String note) {
        String normalized = TextNormalizer.trim(note);
        return (normalized == null || normalized.isBlank()) ? null : normalized;
    }
}