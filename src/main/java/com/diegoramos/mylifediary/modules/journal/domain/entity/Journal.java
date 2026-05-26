package com.diegoramos.mylifediary.modules.journal.domain.entity;

import com.diegoramos.mylifediary.common.base.BaseEntity;
import com.diegoramos.mylifediary.common.util.DomainValidation;
import com.diegoramos.mylifediary.common.util.TextNormalizer;
import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "journals")
public class Journal extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "title", length = 80, nullable = false)
    private String title;
    @Column(name = "is_locked", nullable = false)
    private boolean isLocked;

    private Journal(User user,
                    String title,
                    boolean isLocked) {

        this.user = user;
        this.title = title;
        this.isLocked = isLocked;
    }

    public static Journal create(User user,
                                 String title,
                                 boolean isLocked) {

        DomainValidation.validateRequired(user, "O usuário");
        DomainValidation.validateRequired(title, "O nome do diário");

        return new Journal(user, TextNormalizer.name(title), isLocked);
    }
}
