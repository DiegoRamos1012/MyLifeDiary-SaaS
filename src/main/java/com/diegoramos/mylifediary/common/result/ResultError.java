package com.diegoramos.mylifediary.common.result;

import java.util.List;
import java.util.Objects;

/**
 * Representa um erro esperado dentro do {@link Result}.
 *
 * <p>O código deve ser estável para facilitar integrações e logs, enquanto a
 * mensagem deve ser clara para leitura humana. Os detalhes são opcionais e
 * servem para enriquecer o contexto sem transformar o fluxo em exceção.
 *
 * @param code código estável do erro
 * @param message mensagem legível do erro
 * @param details lista opcional de detalhes
 */
public record ResultError(String code, String message, List<String> details) {

    /**
     * Cria um erro sem detalhes adicionais.
     *
     * @param code código estável do erro
     * @param message mensagem legível do erro
     * @return instância de erro
     */
    public static ResultError of(String code, String message) {
        return new ResultError(code, message, List.of());
    }

    /**
     * Cria um erro com detalhes adicionais.
     *
     * @param code código estável do erro
     * @param message mensagem legível do erro
     * @param details lista de detalhes do erro
     * @return instância de erro
     */
    public static ResultError of(String code, String message, List<String> details) {
        return new ResultError(code, message, details);
    }

    /**
     * Normaliza e valida os dados do erro.
     *
     * @param code código estável do erro
     * @param message mensagem legível do erro
     * @param details lista de detalhes do erro
     */
    public ResultError {
        code = Objects.requireNonNull(code, "code must not be null");
        message = Objects.requireNonNull(message, "message must not be null");
        details = details == null ? List.of() : List.copyOf(details);
    }
}


