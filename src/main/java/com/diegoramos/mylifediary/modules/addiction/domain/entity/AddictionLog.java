package com.diegoramos.mylifediary.modules.addiction.domain.entity;

import com.diegoramos.mylifediary.common.base.BaseEntity;
import com.diegoramos.mylifediary.common.util.DomainValidation;
import com.diegoramos.mylifediary.common.util.TextNormalizer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Registro diário de recuperação ou recaída de uma dependência.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "addiction_logs",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"addiction_id", "date"})
        }
)
public class AddictionLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addiction_id", nullable = false)
    private Addiction addiction;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "relapsed", nullable = false)
    private boolean relapsed;

    @Column(name = "note", length = 500)
    private String note;

    private AddictionLog(Addiction addiction,
                         LocalDate date,
                         boolean relapsed,
                         String note) {
        this.addiction = addiction;
        this.date = date;
        this.relapsed = relapsed;
        this.note = note;
    }

    /**
     * Cria um novo log diário, com validação de entrada e normalização da nota.
     */
    public static AddictionLog create(Addiction addiction,
                                      LocalDate date,
                                      Boolean relapsed,
                                      String note) {
        DomainValidation.validateRequired(addiction, "A dependência");
        DomainValidation.validateRequired(date, "A data");
        DomainValidation.validateRequired(relapsed, "O status de recaída");

        return new AddictionLog(addiction, date, relapsed, normalizeNote(note));
    }

    /**
     * Atualiza um log já existente para refletir recaída ou dia sem recaída.
     */
    public void mark(Boolean relapsed, String note) {
        DomainValidation.validateRequired(relapsed, "O status de recaída");
        this.relapsed = relapsed;
        this.note = normalizeNote(note);
        updateLastTimeChanged();
    }

    private static String normalizeNote(String note) {
        String normalized = TextNormalizer.trim(note);
        return (normalized == null || normalized.isBlank()) ? null : normalized;
    }
}

