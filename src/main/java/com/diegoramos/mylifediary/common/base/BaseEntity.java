package com.diegoramos.mylifediary.common.base;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Executa automaticamente antes do INSERT.
     * Define o {@code id} como UUID v7 e inicializa {@code createdAt} e
     * {@code updatedAt} com o horário atual em UTC.
     */
    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UuidCreator.getTimeOrderedEpoch();
        }
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    /**
     * Executa automaticamente antes da entidade existente receber uma atualização no banco (UPDATE)
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    protected void updateLastTimeChanged() {
        this.updatedAt = Instant.now();
    }
}
