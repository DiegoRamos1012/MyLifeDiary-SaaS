package com.diegoramos.mylifediary.modules.user.repository;

import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import com.diegoramos.mylifediary.modules.user.domain.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de persistência para a entidade {@link User}.
 *
 * <p>Centraliza consultas de leitura, validações de existência e operações em lote
 * utilizadas pelos fluxos de negócio e jobs de ciclo de vida do usuário.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Verifica se já existe usuário com o e-mail informado, ignorando caixa alta/baixa.
     *
     * @param email e-mail a ser buscado
     * @return {@code true} quando já existe usuário com o e-mail informado
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Busca o usuário pelo token
     *
     * @param token token a ser buscado
     * @return {@code true} quando encontra o usuário com este token
     */
    Optional<User> findByVerificationToken(String token);

    java.util.Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Busca usuários por nome parcial (contém), ignorando caixa alta/baixa, com paginação.
     *
     * @param fullName termo parcial do nome completo
     * @param pageable configuração de paginação e ordenação
     * @return página de usuários encontrados
     */
    Page<User> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);

    /**
     * Busca usuários por status, com paginação
     *
     * @param statusList lista de status
     * @param pageable   configuração de paginação e ordenação
     */
    Page<User> findByStatusIn(List<UserStatus> statusList, Pageable pageable);

    /**
     * Atualiza em lote usuários que já passaram da data limite de exclusão pendente,
     * alterando o status de {@code pendingStatus} para {@code inactiveStatus}.
     *
     * @param pendingStatus  status de origem elegível para transição
     * @param inactiveStatus status de destino após a transição
     * @param threshold      data limite para elegibilidade (deletionRequestedAt < threshold)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE User u
               SET u.status = :inactiveStatus
             WHERE u.status = :pendingStatus
               AND u.deletionRequestedAt IS NOT NULL
               AND u.deletionRequestedAt < :threshold
            """)
    void markPendingDeletionUsersAsInactive(
            @Param("pendingStatus") UserStatus pendingStatus,
            @Param("inactiveStatus") UserStatus inactiveStatus,
            @Param("threshold") Instant threshold
    );

    /**
     * Remove definitivamente em lote usuários inativos que já passaram da data limite.
     *
     * @param inactiveStatus status de inatividade elegível para hard delete
     * @param threshold      data limite para elegibilidade (deletionRequestedAt < threshold)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE from User u
             WHERE u.status = :inactiveStatus
               AND u.deletionRequestedAt IS NOT NULL
               AND u.deletionRequestedAt < :threshold
            """)
    void hardDeleteInactiveUsersBefore(
            @Param("inactiveStatus") UserStatus inactiveStatus,
            @Param("threshold") Instant threshold
    );
}
