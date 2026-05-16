package com.diegoramos.mylifediary.common.util;

import com.diegoramos.mylifediary.common.exception.DomainException;

import java.util.Collection;
import java.util.Map;

/**
 * Classe utilitária responsável por realizar validações
 * comuns de domínio.
 *
 * <p>
 * Atualmente fornece validação para campos obrigatórios,
 * suportando diferentes tipos de dados como:
 * </p>
 *
 * <ul>
 *     <li>{@link String} vazia ou em branco</li>
 *     <li>{@link Collection} vazia</li>
 *     <li>{@link Map} vazio</li>
 *     <li>Valores {@code null}</li>
 * </ul>
 *
 * <p>
 * Caso a validação falhe, uma {@link DomainException}
 * será lançada.
 * </p>
 */
public final class DomainValidation {

    /**
     * Construtor privado para impedir instanciação da classe utilitária.
     */
    private DomainValidation() {
    }

    /**
     * Valida se um valor obrigatório foi informado.
     *
     * <p>
     * As seguintes validações são aplicadas:
     * </p>
     *
     * <ul>
     *     <li>Valor {@code null}</li>
     *     <li>{@link String} em branco</li>
     *     <li>{@link Collection} vazia</li>
     *     <li>{@link Map} vazio</li>
     * </ul>
     *
     * @param value     valor a ser validado
     * @param fieldName nome do campo utilizado na mensagem de erro
     * @throws DomainException caso o valor seja inválido
     */
    public static void validateRequired(Object value, String fieldName) {

        switch (value) {

            case null -> throw new DomainException(fieldName + " não pode estar vazio");

            case String str when str.isBlank() -> throw new DomainException(fieldName + " não pode estar vazio");

            case Collection<?> collection when collection.isEmpty() ->
                    throw new DomainException(fieldName + " não pode estar vazio");

            case Map<?, ?> map when map.isEmpty() -> throw new DomainException(fieldName + " não pode estar vazio");

            default -> {
            }
        }
    }
}