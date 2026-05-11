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

    /**
     * Construtor interno usado pela fábrica {@link #create(String, String, String, LocalDate)}.
     *
     * @param fullName     nome completo do usuário
     * @param email        e-mail normalizado
     * @param passwordHash hash da senha
     * @param birthDate    data de nascimento (opcional)
     * @param status       status inicial do usuário
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
     */
    public static User create(String email, String passwordHash, String fullName, LocalDate birthDate) {
        if (email == null || email.isBlank()) {
            throw new DomainException("Erro: E-mail não pode estar vazio");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new DomainException("Erro: Senha inválida");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new DomainException("Erro: Nome não pode estar vazio");
        }

        // Constructor expects (fullName, email, passwordHash, birthDate, status)
        return new User(normalizeFullName(fullName), normalizeEmail(email), passwordHash, birthDate, UserStatus.ACTIVE);
    }

    /**
     * Normaliza o e-mail para armazenamento e comparação: trim + lowercase.
     *
     * @param email e-mail bruto
     * @return e-mail normalizado
     */
    private static String normalizeEmail(@NonNull String email) {
        return email.trim().toLowerCase();
    }

    /**
     * Normaliza o nome completo removendo espaços laterais.
     *
     * @param fullName nome bruto
     * @return nome normalizado
     */
    private static String normalizeFullName(@NonNull String fullName) {
        return fullName.trim();
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

    /**
     * Marca o usuário como inativo (hard state) — usado por jobs/rotinas de
     * limpeza que convertem contas pendentes em inativas.
     */
    public void markAsInactive() {
        this.status = UserStatus.INACTIVE;
    }

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
        this.email = normalizeEmail(newEmail);
        updateLastTimeChanged();
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
            throw new DomainException("Erro: Senha inválida");
        }
        this.passwordHash = newPasswordHash;
        updateLastTimeChanged();
    }

    /**
     * Atualiza informações do perfil do usuário (nome e data de nascimento).
     *
     * @param fullName  novo nome completo (obrigatório)
     * @param birthDate nova data de nascimento (opcional)
     * @throws DomainException quando o nome for nulo ou vazio
     */
    public void changeProfileInfo(String fullName, LocalDate birthDate) {
        if (fullName == null || fullName.isBlank()) {
            throw new DomainException("Erro: Nome inválido");
        }
        this.fullName = normalizeFullName(fullName);
        this.birthDate = birthDate;
    }
}
