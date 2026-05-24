package com.diegoramos.mylifediary.modules.habit.controller;
import com.diegoramos.mylifediary.common.response.ResultHttpResponseHelper;
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
@Tag(name = "Habits", description = "Cadastro de habitos, logs diarios e streak")
@SecurityRequirement(name = "bearerAuth")
public class HabitController {
    private final HabitService habitService;
    /** Cria o controller com o serviço de hábitos. */
    public HabitController(HabitService habitService) {
        this.habitService = habitService;
    }
    @PostMapping("/users/{userId}")
    @Operation(summary = "Cria um novo habito para um usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Habito criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
    })
    /** Cria um novo hábito para o usuário informado. */
    public ResponseEntity<?> createHabit(@PathVariable UUID userId,
                                         @RequestBody @Valid CreateHabitRequest request) {
        return ResultHttpResponseHelper.respond(habitService.createHabit(userId, request), HttpStatus.CREATED);
    }
    @PutMapping("/{habitId}/logs")
    @Operation(summary = "Cria ou atualiza o log diario do habito")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Log registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "404", description = "Habito nao encontrado")
    })
    /** Registra ou atualiza a execução diária de um hábito. */
    public ResponseEntity<?> markHabitDay(@PathVariable UUID habitId,
                                          @RequestBody @Valid MarkHabitDayRequest request) {
        return ResultHttpResponseHelper.respond(habitService.markHabitDay(habitId, request), HttpStatus.OK);
    }
    @GetMapping("/{habitId}/logs")
    @Operation(summary = "Busca historico de logs do habito")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historico retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Intervalo de datas invalido"),
            @ApiResponse(responseCode = "404", description = "Habito nao encontrado")
    })
    /** Retorna o histórico de logs de um hábito, com filtro opcional por período. */
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
    /** Retorna a streak atual calculada a partir dos logs do hábito. */
    public ResponseEntity<?> getHabitStreak(@PathVariable UUID habitId) {
        return ResultHttpResponseHelper.respond(habitService.getHabitStreak(habitId), HttpStatus.OK);
    }
}
