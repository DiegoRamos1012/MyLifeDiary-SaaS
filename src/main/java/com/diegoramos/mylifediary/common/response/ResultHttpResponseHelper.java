package com.diegoramos.mylifediary.common.response;

import com.diegoramos.mylifediary.common.result.Result;
import com.diegoramos.mylifediary.common.result.ResultError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

/**
 * Helper responsável por converter um {@link Result} em resposta HTTP padronizada.
 *
 * <p>Ele mantém os controllers enxutos ao centralizar a tradução entre:
 *
 * <ul>
 *   <li>sucesso de domínio -> resposta HTTP de sucesso;</li>
 *   <li>falha esperada -> {@link ApiErrorResponse} com status apropriado.</li>
 * </ul>
 *
 * <p>Falhas inesperadas continuam sob responsabilidade do {@code GlobalExceptionHandler}.
 */
public final class ResultHttpResponseHelper {

    private ResultHttpResponseHelper() {
    }

    /**
     * Converte um {@link Result} em {@link ResponseEntity}.
     *
     * <p>Em caso de sucesso, retorna a resposta com o status informado.
     * Em caso de falha esperada, retorna um {@link ApiErrorResponse} com o status mapeado.
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
                    return ResponseEntity.status(errorStatus).body(new ApiErrorResponse(
                            Instant.now(),
                            errorStatus.value(),
                            errorStatus.getReasonPhrase(),
                            error.message(),
                            request.getRequestURI(),
                            error.details()
                    ));
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
}



