package com.diegoramos.mylifediary.modules.user.domain.enums;

/**
 * Representa o status atual de um usuário dentro do sistema.
 *
 * <p>O status é utilizado para controlar o ciclo de vida do usuário,
 * influenciando o seu acesso e comportamento dentro da aplicação.</p>
 *
 * <p>Fluxo típico de estados:</p>
 * <ul>
 *     <li>ACTIVE → usuário ativo e com acesso normal</li>
 *     <li>PENDING_DELETION → usuário solicitou exclusão da conta</li>
 *     <li>INACTIVE → conta desativada (manual ou automática)</li>
 *     <li>SUSPENDED → conta bloqueada por alguma restrição</li>
 * </ul>
 */
public enum UserStatus {

    /**
     * Usuário ativo no sistema.
     * Possui acesso completo às funcionalidades disponíveis.
     */
    ACTIVE,

    /**
     * Usuário inativo.
     * Pode representar desativação voluntária ou administrativa.
     */
    INACTIVE,

    /**
     * Usuário solicitou exclusão da conta.
     * Geralmente associado ao campo {@code deletionRequestedAt}.
     * Pode haver um período de espera antes da remoção definitiva.
     */
    PENDING_DELETION,

    /**
     * Usuário suspenso.
     * Indica bloqueio por violação de regras ou outras restrições.
     */
    SUSPENDED
}