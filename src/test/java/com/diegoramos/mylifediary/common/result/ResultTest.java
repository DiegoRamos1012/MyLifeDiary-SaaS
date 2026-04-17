package com.diegoramos.mylifediary.common.result;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    @Test
    void successShouldExposeValue() {
        Result<String> result = Result.success("ok");

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals("ok", result.getValue());
        assertEquals("ok", result.orElse("fallback"));
    }

    @Test
    void failureShouldExposeError() {
        Result<String> result = Result.failure("NOT_FOUND", "Registro não encontrado");

        assertTrue(result.isFailure());
        assertFalse(result.isSuccess());
        assertEquals("NOT_FOUND", result.getError().code());
        assertEquals("Registro não encontrado", result.getError().message());
    }

    @Test
    void mapShouldTransformSuccessfulValue() {
        Result<Integer> result = Result.success(2).map(value -> value * 3);

        assertTrue(result.isSuccess());
        assertEquals(6, result.getValue());
    }

    @Test
    void flatMapShouldChainSuccessfulResults() {
        Result<String> result = Result.success(10)
                .flatMap(value -> Result.success("valor=" + value));

        assertTrue(result.isSuccess());
        assertEquals("valor=10", result.getValue());
    }

    @Test
    void foldShouldConvertBothStates() {
        String success = Result.success("A")
                .fold(value -> "sucesso:" + value, error -> "falha:" + error.code());
        String failure = Result.<String>failure("INVALID", "Valor inválido")
                .fold(value -> "sucesso:" + value, error -> "falha:" + error.code());

        assertEquals("sucesso:A", success);
        assertEquals("falha:INVALID", failure);
    }

    @Test
    void detailsShouldBeImmutable() {
        ResultError error = ResultError.of("INVALID", "Erro", List.of("campo obrigatório"));

        assertThrows(UnsupportedOperationException.class, () -> error.details().add("outro detalhe"));
    }
}

