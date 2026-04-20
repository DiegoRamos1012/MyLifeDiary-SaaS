package com.diegoramos.mylifediary.modules.user.domain.entity;

import com.diegoramos.mylifediary.common.base.BaseEntity;
import com.diegoramos.mylifediary.common.exception.DomainException;
import com.diegoramos.mylifediary.modules.user.domain.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    private UserStatus status;

    @Column(name = "deletion_requested_at")
    private Instant deletionRequestedAt;

    /*
    @Column(name = stripe_customer_id)
    private String stripeCustomerId;
    */

    private User(String fullName,
                 String email,
                 String passwordHash,
                 LocalDate birthDate,
                 UserStatus status) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.status = status;
    }

    public static User create(String email, String passwordHash, String fullName, LocalDate birthdayDate) {
        if (email == null || email.isBlank()) {
            throw new DomainException("Erro: E-mail não pode estar vazio");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new DomainException("Erro: Senha inválida");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new DomainException("Erro: Nome não pode estar vazio");
        }

        return new User(normalizeEmail(email), passwordHash, normalizeFullName(fullName), birthdayDate, UserStatus.ACTIVE);
    }

    private static String normalizeEmail(@NonNull String email) {
        return email.trim().toLowerCase();
    }

    private static String normalizeFullName(@NonNull String fullName) {
        return fullName.trim();
    }

    public void requestDeletion(Instant now) {
        this.deletionRequestedAt = now;
        this.status = UserStatus.PENDING_DELETION;
    }

    public void markAsInactive() {
        this.status = UserStatus.INACTIVE;
    }

    public void updateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new DomainException("Erro: E-mail inválido");
        }
        this.email = normalizeEmail(email);
    }

    public void updatePassword(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new DomainException("Erro: Senha inválida");
        }
        this.passwordHash = passwordHash;
    }

    public void updateProfileInfo(String fullName, LocalDate birthdayDate) {
        if (fullName == null || fullName.isBlank()) {
            throw new DomainException("Erro: Nome inválido");
        }
        this.fullName = normalizeFullName(fullName);
        this.birthDate = birthdayDate;
    }
}
