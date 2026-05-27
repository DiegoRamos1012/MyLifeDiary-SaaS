package com.diegoramos.mylifediary.modules.journal.domain.entity;

import com.diegoramos.mylifediary.common.base.BaseEntity;
import com.diegoramos.mylifediary.common.exception.DomainException;
import com.diegoramos.mylifediary.common.util.DomainValidation;
import com.diegoramos.mylifediary.modules.journal.domain.enums.MoodTypes;
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
 * Entidade de nota diária de um {@link Journal}.
 *
 * <p>Cada entrada representa um registro textual de um dia específico,
 * com humor associado. A regra de unicidade por diário e data é reforçada
 * pelo {@code unique constraint} em {@code (journal_id, date)}.</p>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "journal_entrys",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"journal_id", "date"})
        }
)
public class JournalEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id", nullable = false)
    private Journal journal;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "mood", nullable = false)
    private MoodTypes mood;

    private JournalEntry(Journal journal,
                         LocalDate entryDate,
                         String content,
                         MoodTypes mood) {
        this.journal = journal;
        this.entryDate = entryDate;
        this.content = content;
        this.mood = mood;
    }

    /**
     * Fábrica para criação de uma nota diária.
     *
     * @param journal   diário dono da nota
     * @param entryDate data de referência da nota
     * @param content   conteúdo textual
     * @param mood      humor associado ao dia
     * @return instância válida de {@link JournalEntry}
     * @throws DomainException quando os dados forem inválidos
     */
    public static JournalEntry create(Journal journal,
                                      LocalDate entryDate,
                                      String content,
                                      MoodTypes mood) {

        DomainValidation.validateRequired(journal, "O diário");
        DomainValidation.validateRequired(entryDate, "A data da entrada");
        DomainValidation.validateRequired(content, "O conteúdo do diário");
        DomainValidation.validateRequired(mood, "O humor do dia");

        if (content.length() > 20000) {
            throw new DomainException("O limite máximo do diário é de 20.000 caracteres");
        }

        return new JournalEntry(journal, entryDate, content.trim(), mood);
    }

    /**
     * Atualiza conteúdo e humor da nota preservando a data original.
     *
     * @param content novo conteúdo
     * @param mood    novo humor
     * @throws DomainException quando os dados forem inválidos
     */
    public void update(String content, MoodTypes mood) {
        DomainValidation.validateRequired(content, "O conteúdo do diário");
        DomainValidation.validateRequired(mood, "O humor do dia");

        if (content.length() > 20000) {
            throw new DomainException("O limite máximo do diário é de 20.000 caracteres");
        }

        this.content = content.trim();
        this.mood = mood;
    }
}
