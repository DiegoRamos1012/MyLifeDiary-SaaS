package com.diegoramos.mylifediary.common.exception;

/**
 * Exceção de domínio para sinalizar falhas inesperadas na camada de regras de negócio.
 * <p>
 * No projeto, erros esperados devem ser representados por meio do Result Pattern.
 * Esta exceção deve ser usada apenas para cenários excepcionais não previstos no fluxo normal.
 */
public class DomainException extends RuntimeException {

    /**
     * Cria uma exceção de domínio com mensagem padrão.
     */
    public DomainException() {
        super("Erro detectado na camada de domínio");
    }

    /**
     * Cria uma exceção de domínio com mensagem personalizada.
     *
     * @param message descrição do erro ocorrido
     */
    public DomainException(String message) {
        super(message);
    }

    /**
     * Cria uma exceção de domínio com mensagem personalizada e causa original.
     *
     * @param message descrição do erro ocorrido
     * @param cause exceção original que gerou esta falha
     */
    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
