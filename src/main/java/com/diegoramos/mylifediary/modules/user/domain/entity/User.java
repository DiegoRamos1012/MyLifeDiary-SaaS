package com.diegoramos.mylifediary.modules.user.domain.entity;

import com.diegoramos.mylifediary.common.base.BaseEntity;
import com.diegoramos.mylifediary.modules.user.domain.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "birthday_date")
    private LocalDate birthdayDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    private UserStatus status;

    @Column(name = "deletion_requested_at")
    private Instant deletionRequestedAt;


    /*
    @Column(name = stripe_customer_id)
    private String stripeCustomerId;
    */

    private User(String email,
                 String passwordHash,
                 String fullName,
                 LocalDate birthdayDate,
                 UserStatus status) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.birthdayDate = birthdayDate;
        this.status = status;
    }

    public static @NonNull User create(String email, String passwordHash, String fullName, LocalDate birthdayDate) {
        return new User(email, passwordHash, fullName, birthdayDate, UserStatus.ACTIVE);
    }
}
