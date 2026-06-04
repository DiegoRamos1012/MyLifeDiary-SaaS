package com.diegoramos.mylifediary.common.util;

import java.util.Locale;

/**
 * Utility class responsável por normalização de textos simples.
 *
 * <p>Centraliza operações comuns de padronização de Strings utilizadas
 * em diferentes partes do sistema, garantindo consistência no armazenamento
 * e evitando duplicação de lógica de limpeza de entrada.</p>
 *
 * <p>Esta classe NÃO contém validações de domínio, apenas transformações
 * técnicas de input (sanitização).</p>
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /**
     * Remove espaços em branco do início e fim da string.
     *
     * <p>Não altera espaços internos.</p>
     *
     * <b>Exemplo:</b>
     * <pre>
     * Antes:  "  João Silva  "
     * Depois: "João Silva"
     * </pre>
     *
     * @param value texto original
     * @return texto sem espaços nas extremidades ou null se entrada for null
     */
    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * Remove espaços nas extremidades e converte texto vazio/em branco para null.
     *
     * <p>Útil para campos opcionais, mantendo um único significado para valor ausente.</p>
     *
     * @param value texto original
     * @return texto normalizado ou null quando ausente/em branco
     */
    public static String trimToNull(String value) {
        String normalized = trim(value);
        return (normalized == null || normalized.isBlank()) ? null : normalized;
    }

    /**
     * Converte toda a string para letras minúsculas utilizando {@link Locale#ROOT}.
     *
     * <p>Garante comportamento consistente independente do idioma do sistema operacional.</p>
     *
     * <b>Exemplo:</b>
     * <pre>
     * Antes: "João SILVA"
     * Depois: "joão silva"
     * </pre>
     *
     * @param value texto original
     * @return texto em lowercase ou null se entrada for null
     */
    public static String lowercase(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    /**
     * Normaliza um e-mail aplicando:
     * <ul>
     *   <li>Remoção de espaços nas extremidades (trim)</li>
     *   <li>Conversão para lowercase</li>
     * </ul>
     *
     * <p>Garante padronização para armazenamento e comparação de e-mails.</p>
     *
     * <b>Exemplo:</b>
     * <pre>
     * Antes:  "  João.Silva@Email.COM  "
     * Depois: "joão.silva@email.com"
     * </pre>
     *
     * @param value e-mail bruto
     * @return e-mail normalizado ou null se entrada for null
     */
    public static String email(String value) {
        if (value == null) return null;
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Normaliza nomes removendo espaços desnecessários.
     *
     * <p>Regras aplicadas:</p>
     * <ul>
     *   <li>trim nas extremidades</li>
     *   <li>redução de múltiplos espaços internos para um único espaço</li>
     * </ul>
     *
     * <b>Exemplo:</b>
     * <pre>
     * Antes:  "   João    Silva   da   Costa   "
     * Depois: "João Silva da Costa"
     * </pre>
     *
     * @param value nome bruto
     * @return nome normalizado ou null se entrada for null
     */
    public static String name(String value) {
        if (value == null) return null;
        return value.trim().replaceAll("\\s+", " ");
    }
}