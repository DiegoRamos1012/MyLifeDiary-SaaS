package com.diegoramos.mylifediary.modules.user.domain.entity;

import com.diegoramos.mylifediary.common.base.BaseEntity;
import com.diegoramos.mylifediary.common.exception.DomainException;
import com.diegoramos.mylifediary.common.result.Result;
import com.diegoramos.mylifediary.modules.user.domain.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    public static User create(String email, String passwordHash, String fullName, LocalDate birthdayDate) {
        if (email == null || email.isBlank()) {
            throw new DomainException("Erro:  não pode ser vazio");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new DomainException("Erro: Senha inválida");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new DomainException("Erro: Nome não pode estar vazio");
        }

        return new User(normalizeEmail(email), passwordHash, normalizeFullName(fullName), birthdayDate, UserStatus.ACTIVE);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private static String normalizeFullName(String fullName) {
        return fullName.trim();
    }

    public Result<User> updateEmail(String email) {
        if (email == null || email.isBlank()) {
            return Result.failure("INVALID_EMAIL", "Email inválido");
        }
        this.email = normalizeEmail(email);
        return Result.success(this);
    }

    public Result<User> updatePassword(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            return Result.failure("INVALID_PASSWORD", "Senha inválida");
        }
        this.passwordHash = passwordHash;
        return Result.success(this);
    }

    public Result<User> updateProfile(String fullName, LocalDate birthdayDate) {
        if (fullName == null || fullName.isBlank()) {
            return Result.failure("INVALID_FULL_NAME", "Nome inválido");
        }
        this.fullName = normalizeFullName(fullName);
        this.birthdayDate = birthdayDate;
        return Result.success(this);
    }
}
