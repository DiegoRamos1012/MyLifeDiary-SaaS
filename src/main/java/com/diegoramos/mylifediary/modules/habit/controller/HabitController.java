package com.diegoramos.mylifediary.modules.habit.controller;

import com.diegoramos.mylifediary.common.response.ResultHttpResponseHelper;
import com.diegoramos.mylifediary.config.security.CustomUserDetails;
import com.diegoramos.mylifediary.modules.habit.dto.request.CreateHabitRequest;
import com.diegoramos.mylifediary.modules.habit.dto.request.MarkHabitDayRequest;
import com.diegoramos.mylifediary.modules.habit.service.HabitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller REST do módulo de hábitos.
 *
 * <p>Expõe endpoints para criação de hábitos, marcação de execução diária,
 * consulta de histórico e cálculo de streak.</p>
 */
@RestController
@RequestMapping("/habits")
@Tag(name = "Habits", description = "Cadastro de hábitos, logs diários e progressão")
@SecurityRequirement(name = "bearerAuth")
public class HabitController {

    private final HabitService habitService;

    /**
     * Cria o controller com o serviço de hábitos.
     */
    public HabitController(HabitService habitService) {
        this.habitService = habitService;
    }

    @PostMapping
    @Operation(summary = "Cria um novo hábito")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Hábito criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<?> createHabit(@AuthenticationPrincipal CustomUserDetails currentUser,
                                         @RequestBody @Valid CreateHabitRequest request) {

        UUID userId = currentUser.getId();

        return ResultHttpResponseHelper.respond(habitService.createHabit(userId, request), HttpStatus.CREATED);
    }

    @PutMapping("/{habitId}/logs")
    @Operation(summary = "Cria ou atualiza o log diário do hábito")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Log registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Hábito não encontrado")
    })
    public ResponseEntity<?> markHabitDay(@PathVariable UUID habitId,
                                          @RequestBody @Valid MarkHabitDayRequest request) {
        return ResultHttpResponseHelper.respond(habitService.markHabitDay(habitId, request), HttpStatus.OK);
    }

    @GetMapping("/{habitId}/logs")
    @Operation(summary = "Busca histórico de logs do habito")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Intervalo de datas inválido"),
            @ApiResponse(responseCode = "404", description = "Hábito não encontrado")
    })
    public ResponseEntity<?> getHabitLogs(@PathVariable UUID habitId,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResultHttpResponseHelper.respond(habitService.getHabitLogs(habitId, fromDate, toDate), HttpStatus.OK);
    }

    @GetMapping("/{habitId}/streak")
    @Operation(summary = "Calcula a streak atual do habito")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Streak calculada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Habito nao encontrado")
    })
    public ResponseEntity<?> getHabitStreak(@PathVariable UUID habitId) {
        return ResultHttpResponseHelper.respond(habitService.getHabitStreak(habitId), HttpStatus.OK);
    }
}
