package com.diegoramos.mylifediary.common.result;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Representa o resultado de uma operação de domínio ou de aplicação.
 *
 * <p>Use esta classe para fluxos esperados, como validações de regra de negócio,
 * indisponibilidade de recurso esperado ou decisões de domínio que não devem
 * ser tratadas como exceções. Exceções devem ser reservadas para falhas
 * inesperadas.
 *
 * <p>A classe é imutável e expõe uma API pequena para manter o código simples,
 * legível e fácil de manter.
 *
 * @param <T> tipo do valor de sucesso carregado pelo resultado
 */
public final class Result<T> {

    private final T value;
    private final ResultError error;

    private Result(T value, ResultError error) {
        this.value = value;
        this.error = error;
    }

    /**
     * Cria um resultado de sucesso.
     *
     * @param value valor retornado pela operação
     * @param <T> tipo do valor de sucesso
     * @return resultado representando sucesso
     */
    public static <T> Result<T> success(T value) {
        return new Result<>(value, null);
    }

    /**
     * Cria um resultado de falha com código e mensagem.
     *
     * @param code código estável do erro
     * @param message mensagem legível do erro
     * @param <T> tipo do valor esperado em caso de sucesso
     * @return resultado representando falha
     */
    public static <T> Result<T> failure(String code, String message) {
        return failure(ResultError.of(code, message));
    }

    /**
     * Cria um resultado de falha com código, mensagem e detalhes.
     *
     * @param code código estável do erro
     * @param message mensagem legível do erro
     * @param details lista opcional de detalhes do erro
     * @param <T> tipo do valor esperado em caso de sucesso
     * @return resultado representando falha
     */
    public static <T> Result<T> failure(String code, String message, List<String> details) {
        return failure(ResultError.of(code, message, details));
    }

    /**
     * Cria um resultado de falha a partir de um objeto de erro.
     *
     * @param error descrição do erro
     * @param <T> tipo do valor esperado em caso de sucesso
     * @return resultado representando falha
     */
    public static <T> Result<T> failure(ResultError error) {
        return new Result<>(null, Objects.requireNonNull(error, "error must not be null"));
    }

    /**
     * Informa se a operação foi bem-sucedida.
     *
     * @return {@code true} quando o resultado é de sucesso
     */
    public boolean isSuccess() {
        return error == null;
    }

    /**
     * Informa se a operação falhou.
     *
     * @return {@code true} quando o resultado é de falha
     */
    public boolean isFailure() {
        return !isSuccess();
    }

    /**
     * Obtém o valor de sucesso.
     *
     * @return valor armazenado
     * @throws IllegalStateException se o resultado representar falha
     */
    public T getValue() {
        if (isFailure()) {
            throw new IllegalStateException("Cannot read the value from a failed Result.");
        }
        return value;
    }

    /**
     * Obtém o erro de falha.
     *
     * @return erro armazenado
     * @throws IllegalStateException se o resultado representar sucesso
     */
    public ResultError getError() {
        if (isSuccess()) {
            throw new IllegalStateException("Cannot read the error from a successful Result.");
        }
        return error;
    }

    /**
     * Transforma o valor de sucesso em outro tipo.
     *
     * @param mapper função de transformação
     * @param <U> tipo do novo valor
     * @return um novo {@code Result} com o valor transformado, ou a mesma falha
     */
    public <U> Result<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        if (isFailure()) {
            return Result.failure(error);
        }
        return Result.success(mapper.apply(value));
    }

    /**
     * Transforma o valor de sucesso em outro {@code Result}.
     *
     * @param mapper função de transformação encadeada
     * @param <U> tipo do novo valor
     * @return resultado encadeado ou a mesma falha
     */
    public <U> Result<U> flatMap(Function<? super T, Result<U>> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        if (isFailure()) {
            return Result.failure(error);
        }
        return Objects.requireNonNull(mapper.apply(value), "mapper must not return null");
    }

    /**
     * Converte o resultado em um valor final.
     *
     * @param onSuccess função aplicada quando houver sucesso
     * @param onFailure função aplicada quando houver falha
     * @param <R> tipo do valor retornado
     * @return valor convertido a partir do estado do resultado
     */
    public <R> R fold(Function<? super T, ? extends R> onSuccess,
                      Function<? super ResultError, ? extends R> onFailure) {
        Objects.requireNonNull(onSuccess, "onSuccess must not be null");
        Objects.requireNonNull(onFailure, "onFailure must not be null");
        return isSuccess() ? onSuccess.apply(value) : onFailure.apply(error);
    }

    /**
     * Retorna o valor de sucesso ou um valor padrão.
     *
     * @param fallback valor padrão
     * @return valor de sucesso ou o fallback
     */
    public T orElse(T fallback) {
        return isSuccess() ? value : fallback;
    }

    /**
     * Retorna o valor de sucesso ou um valor gerado sob demanda.
     *
     * @param supplier fornecedor do valor padrão
     * @return valor de sucesso ou o valor fornecido
     */
    public T orElseGet(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        return isSuccess() ? value : supplier.get();
    }

    /**
     * Retorna o valor de sucesso ou lança uma exceção personalizada.
     *
     * @param exceptionSupplier fornecedor da exceção
     * @return valor de sucesso
     */
    public T orElseThrow(Supplier<? extends RuntimeException> exceptionSupplier) {
        Objects.requireNonNull(exceptionSupplier, "exceptionSupplier must not be null");
        if (isFailure()) {
            throw exceptionSupplier.get();
        }
        return value;
    }
}

