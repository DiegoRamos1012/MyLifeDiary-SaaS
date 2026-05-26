package com.diegoramos.mylifediary.modules.journal.domain.entity;

import com.diegoramos.mylifediary.common.base.BaseEntity;
import com.diegoramos.mylifediary.common.exception.DomainException;
import com.diegoramos.mylifediary.common.util.DomainValidation;
import com.diegoramos.mylifediary.common.util.TextNormalizer;
import com.diegoramos.mylifediary.modules.journal.domain.enums.MoodTypes;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "mood", nullable = false)
    private MoodTypes mood;

    private JournalEntry(Journal journal,
                         String content,
                         MoodTypes mood) {
        this.journal = journal;
        this.content = content;
        this.mood = mood;
    }

    private static JournalEntry create(Journal journal,
                                       String content,
                                       MoodTypes mood) {

        DomainValidation.validateRequired(journal, "O diário");
        DomainValidation.validateRequired(content, "O conteúdo do diário");
        DomainValidation.validateRequired(mood, "O humor do dia");

        if (content.length() > 20000) {
            throw new DomainException("O limite máximo do diário é de 20.000 caracteres")
        }

        return new JournalEntry(journal, content, mood );
    }
}
