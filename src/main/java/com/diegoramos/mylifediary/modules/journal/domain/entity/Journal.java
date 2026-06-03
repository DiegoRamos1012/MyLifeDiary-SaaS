package com.diegoramos.mylifediary.modules.journal.domain.entity;

import com.diegoramos.mylifediary.common.base.BaseEntity;
import com.diegoramos.mylifediary.common.exception.DomainException;
import com.diegoramos.mylifediary.common.util.DomainValidation;
import com.diegoramos.mylifediary.common.util.TextNormalizer;
import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Agregado de diário do usuário.
 *
 * <p>Representa um diário pertencente a um único {@link User}, com suporte a
 * proteção por senha através de hash. A senha é armazenada apenas como hash e
 * nunca como texto puro.</p>
 */
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

    @JsonIgnore
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    private Journal(User user,
                    String title,
                    boolean isLocked,
                    String passwordHash) {
        this.user = user;
        this.title = title;
        this.isLocked = isLocked;
        this.passwordHash = passwordHash;
    }

    /**
     * Fábrica para criação de diário com validações de domínio.
     *
     * @param user     dono do diário
     * @param title    título do diário
     * @param isLocked estado inicial de bloqueio
     * @return instância de diário válida
     * @throws DomainException quando dados obrigatórios forem inválidos
     */
    public static Journal create(User user,
                                 String title,
                                 boolean isLocked) {

        DomainValidation.validateRequired(user, "O usuário");
        DomainValidation.validateRequired(title, "O nome do diário");

        return new Journal(user, TextNormalizer.name(title), isLocked, null);
    }

    /**
     * Renomeia o diário aplicando normalização de texto.
     *
     * @param title novo título
     * @throws DomainException quando o título for inválido
     */
    public void rename(String title) {
        DomainValidation.validateRequired(title, "O nome do diário");
        this.title = TextNormalizer.name(title);
    }

    /**
     * Tranca o diário com hash de senha já calculado pela camada de serviço.
     *
     * @param passwordHash hash Argon da senha
     * @throws DomainException quando o hash for inválido
     */
    public void lockWithPasswordHash(String passwordHash) {
        DomainValidation.validateRequired(passwordHash, "A senha do diário");
        this.isLocked = true;
        this.passwordHash = passwordHash;
    }

    /**
     * Destranca o diário e remove o hash de senha armazenado.
     *
     * @throws DomainException quando o diário já estiver destrancado
     */
    public void unlock() {
        if (!isLocked) {
            throw new DomainException("O diário já está destrancado");
        }
        this.isLocked = false;
        this.passwordHash = null;
    }
}
