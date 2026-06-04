package com.diegoramos.mylifediary.modules.addiction.domain.entity;

import com.diegoramos.mylifediary.common.base.BaseEntity;
import com.diegoramos.mylifediary.common.exception.DomainException;
import com.diegoramos.mylifediary.common.util.DomainValidation;
import com.diegoramos.mylifediary.common.util.TextNormalizer;
import com.diegoramos.mylifediary.modules.addiction.domain.enums.AddictionCategory;
import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Agregado que representa um comportamento que o usuário deseja evitar ou superar.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "addictions")
public class Addiction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", length = 80, nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "addiction_category", nullable = false)
    private AddictionCategory addictionCategory;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    private Addiction(User user,
                      String title,
                      String description,
                      AddictionCategory addictionCategory,
                      LocalDate startDate) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.addictionCategory = addictionCategory;
        this.startDate = startDate;
    }

    /**
     * Cria uma nova dependência/objetivo de superação com validações de domínio.
     */
    public static Addiction create(User user,
                                   String title,
                                   String description,
                                   AddictionCategory addictionCategory,
                                   LocalDate startDate) {
        DomainValidation.validateRequired(user, "O usuário");
        DomainValidation.validateRequired(title, "O título da dependência");
        DomainValidation.validateRequired(addictionCategory, "A categoria da dependência");
        DomainValidation.validateRequired(startDate, "A data de início");

        return new Addiction(
                user,
                TextNormalizer.name(title),
                TextNormalizer.trimToNull(description),
                addictionCategory,
                startDate
        );
    }
}


