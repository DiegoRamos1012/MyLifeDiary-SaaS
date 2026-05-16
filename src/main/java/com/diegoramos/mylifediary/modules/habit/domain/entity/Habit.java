package com.diegoramos.mylifediary.modules.habit.domain.entity;

import com.diegoramos.mylifediary.common.base.BaseEntity;
import com.diegoramos.mylifediary.common.util.DomainValidation;
import com.diegoramos.mylifediary.modules.habit.domain.enums.HabitCategory;
import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "habits")
public class Habit extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", length = 80, nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "habit_category", nullable = false)
    private HabitCategory habitCategory;

    @Column(name = "goal_daily", nullable = false)
    private Integer goalDaily;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    public Habit(User user,
                 String title,
                 String description,
                 HabitCategory habitCategory,
                 Integer goalDaily,
                 LocalDate startDate) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.habitCategory = habitCategory;
        this.goalDaily = goalDaily;
        this.startDate = startDate;
    }

    public static Habit create(
            User user,
            String title,
            String description,
            HabitCategory habitCategory,
            Integer goalDaily,
            LocalDate startDate
    ) {

        DomainValidation.validateRequired(user, "O usuário");
        DomainValidation.validateRequired(title, "O título do hábito");
        DomainValidation.validateRequired(habitCategory, "A categoria do hábito");
        DomainValidation.validateRequired(startDate, "A data de início");

        return new Habit(
                user,
                title,
                description,
                habitCategory,
                goalDaily,
                startDate
        );
    }

    
}
