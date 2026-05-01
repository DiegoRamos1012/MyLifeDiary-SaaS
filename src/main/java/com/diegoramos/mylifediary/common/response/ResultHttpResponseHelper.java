package com.diegoramos.mylifediary.common.response;

import com.diegoramos.mylifediary.common.result.Result;
import com.diegoramos.mylifediary.common.result.ResultError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

/**
 * Helper responsável por converter um {@link Result} em uma resposta HTTP padronizada.
 *
 * <p>O objetivo é manter os controllers enxutos, centralizando a tradução entre:
 *
 * <ul>
 *   <li>sucesso de domínio -> resposta HTTP de sucesso;</li>
 *   <li>falha esperada -> {@link ApiErrorResponse} com status apropriado.</li>
 * </ul>
 *
 * <p>Este helper deve ser usado apenas para erros esperados de negócio. Falhas inesperadas
 * continuam sendo tratadas pelo {@code GlobalExceptionHandler}.
 */
public final class ResultHttpResponseHelper {

    private ResultHttpResponseHelper() {
    }

    /**
     * Converte um {@link Result} em {@link ResponseEntity} com status de sucesso e erro padronizado.
     *
     * @param result o resultado retornado pelo service
     * @param successStatus o status HTTP usado quando o resultado for de sucesso
     * @param request a requisição HTTP atual, usada para preencher o caminho no erro
     * @param <T> tipo do valor de sucesso carregado pelo {@link Result}
     * @return uma resposta HTTP pronta para uso no controller
     */
    public static <T> ResponseEntity<?> respond(Result<T> result,
                                                HttpStatus successStatus,
                                                HttpServletRequest request) {
        return result.fold(
                value -> ResponseEntity.status(successStatus).body(value),
                error -> {
                    HttpStatus errorStatus = mapErrorStatus(error);
                    return ResponseEntity
                            .status(errorStatus)
                            .body(buildErrorResponse(error, request, errorStatus));
                }
        );
    }

    /**
     * Mapeia um código de erro esperado para o status HTTP correspondente.
     *
     * @param error erro de domínio retornado pelo service
     * @return status HTTP apropriado para a falha informada
     */
    private static HttpStatus mapErrorStatus(ResultError error) {
        return switch (error.code()) {
            case "USER_EMAIL_ALREADY_EXISTS" -> HttpStatus.CONFLICT;
            case "USER_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    /**
     * Monta o corpo padronizado de erro da API.
     *
     * @param error erro de domínio retornado pelo service
     * @param request requisição HTTP atual
     * @param status status HTTP resolvido para o erro
     * @return corpo de erro da API pronto para serialização
     */
    private static ApiErrorResponse buildErrorResponse(ResultError error,
                                                       HttpServletRequest request,
                                                       HttpStatus status) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                error.message(),
                request.getRequestURI(),
                error.details()
        );
    }
}

