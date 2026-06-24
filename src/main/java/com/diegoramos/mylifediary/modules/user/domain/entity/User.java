package com.diegoramos.mylifediary.modules.user.domain.entity;

import com.diegoramos.mylifediary.common.base.BaseEntity;
import com.diegoramos.mylifediary.common.exception.DomainException;
import com.diegoramos.mylifediary.common.util.DomainValidation;
import com.diegoramos.mylifediary.common.util.TextNormalizer;
import com.diegoramos.mylifediary.modules.user.domain.enums.UserRole;
import com.diegoramos.mylifediary.modules.user.domain.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Agregado de usuário (entidade de domínio).
 *
 * <p>Responsável por manter as invariantes e regras locais do usuário. Operações
 * que representam validações esperadas lançam {@link DomainException} — conforme
 * decisão do projeto, o {@code Result} será utilizado na camada de serviço para
 * representar fluxos esperados. Métodos que alteram estado atualizam campos e
 * também registram metadados (ex.: última alteração) quando aplicável.</p>
 *
 * <p>A classe é uma JPA {@code @Entity} mapeada para a tabela {@code users}.</p>
 */
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
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "verification_token_expires_at")
    private Instant verificationTokenExpiresAt;

    @Column(name = "deletion_requested_at")
    private Instant deletionRequestedAt;

    /*
    @Column(name = stripe_customer_id)
    private String stripeCustomerId;
    */

    /**
     * Construtor interno usado pela fábrica {@link (String, String, String, LocalDate, UserStatus, UserRole, boolean)}.
     *
     * @param fullName      nome completo do usuário
     * @param email         e-mail normalizado
     * @param passwordHash  hash da senha
     * @param birthDate     data de nascimento (opcional)
     * @param status        status inicial do usuário
     * @param role          role atual do usuário
     * @param emailVerified status da verificação do email
     */
    private User(String email,
                 String passwordHash,
                 String fullName,
                 LocalDate birthDate,
                 UserStatus status,
                 UserRole role,
                 boolean emailVerified) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.status = status;
        this.role = role;
        this.emailVerified = emailVerified;
    }

    /**
     * Fábrica para criar um usuário com validações básicas de presença.
     *
     * <p>Validações leves são feitas aqui e, em caso de falha, uma
     * {@link DomainException} é lançada — o serviço chamador é responsável por
     * capturar e converter para {@code Result} quando necessário.</p>
     *
     * @param email        e-mail do usuário (obrigatório)
     * @param passwordHash hash da senha já gerado (obrigatório)
     * @param fullName     nome completo do usuário (obrigatório)
     * @param birthDate    data de nascimento (opcional)
     * @return instância de {@link User} preparada com status {@code ACTIVE}
     * @throws DomainException quando algum campo obrigatório estiver ausente ou inválido
     * @
     */
    public static User create(String email,
                              String passwordHash,
                              String fullName,
                              LocalDate birthDate
    ) {

        DomainValidation.validateRequired(email, "E-mail");
        DomainValidation.validateRequired(passwordHash, "Senha");
        DomainValidation.validateRequired(fullName, "Nome");

        return new User(
                TextNormalizer.email(email),
                passwordHash,
                TextNormalizer.name(fullName),
                birthDate,
                UserStatus.ACTIVE,
                UserRole.USER,
                false
        );
    }

    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Marca a conta como pendente de exclusão, registrando o instante da
     * solicitação.
     *
     * @param now instante em que a solicitação foi feita
     */
    public void requestDeletion(Instant now) {
        this.deletionRequestedAt = now;
        this.status = UserStatus.PENDING_DELETION;
    }

    /*
      Marca o usuário como inativo (hard state) — usado por jobs/rotinas de
      limpeza que convertem contas pendentes em inativas.

     public void markAsInactive() {
        this.status = UserStatus.INACTIVE;
    } */

    /**
     * Restaura a conta para o estado {@code ACTIVE} e limpa a marcação de
     * exclusão pendente.
     */
    public void restoreAccount() {
        this.status = UserStatus.ACTIVE;
        this.deletionRequestedAt = null;
    }

    /**
     * Atualiza o e-mail do usuário aplicando normalização e validando a
     * presença. Lança {@link DomainException} em caso de entrada inválida.
     *
     * @param newEmail novo e-mail
     * @throws DomainException quando o e-mail for nulo ou vazio
     */
    public void changeEmail(String newEmail) {
        if (newEmail == null || newEmail.isBlank()) {
            throw new DomainException("Erro: E-mail inválido");
        }
        this.email = TextNormalizer.email(newEmail);
        updateUpdatedAt();
    }

    /**
     * Altera a senha (recebe hash já gerado). Lança {@link DomainException}
     * quando o hash informado for inválido.
     *
     * @param newPasswordHash hash da nova senha
     * @throws DomainException quando o hash for nulo ou vazio
     */
    public void changePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new DomainException("Erro: senha inválida");
        }
        this.passwordHash = newPasswordHash;
        updateUpdatedAt();
    }

    /**
     * Atualiza informações do perfil do usuário (nome e data de nascimento).
     *
     * @param fullName  novo nome completo (obrigatório)
     * @param birthDate nova data de nascimento (opcional)
     * @param today     data atual do Clock
     * @throws DomainException quando o nome for nulo ou vazio
     */
    public void changeProfileInfo(String fullName, LocalDate birthDate, LocalDate today) {
        if (fullName == null || fullName.isBlank()) {
            throw new DomainException("Erro: nome inválido");
        }

        if (birthDate == null || birthDate.isAfter(today)) {
            throw new DomainException("Erro: data inválida");
        }

        this.fullName = TextNormalizer.name(fullName);
        this.birthDate = birthDate;
    }

    public void assignVerificationToken(String token, Instant expiredAt) {
        this.verificationToken = token;
        this.verificationTokenExpiresAt = expiredAt;
    }

    public void confirmEmailVerification() {
        this.emailVerified = true;
        this.verificationToken = null;
        this.verificationTokenExpiresAt = null;
    }
}
