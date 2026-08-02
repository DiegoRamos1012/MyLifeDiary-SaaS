package com.diegoramos.mylifediary.modules.addiction.controller;

import com.diegoramos.mylifediary.common.response.ResultHttpResponseHelper;
import com.diegoramos.mylifediary.config.jwt.JwtService;
import com.diegoramos.mylifediary.modules.addiction.dto.request.CreateAddictionRequest;
import com.diegoramos.mylifediary.modules.addiction.dto.request.RegisterAddictionLogRequest;
import com.diegoramos.mylifediary.modules.addiction.service.AddictionService;
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
 * Controller REST do módulo de dependências.
 */
@RestController
@RequestMapping("/addictions")
@Tag(name = "Addictions", description = "Cadastro de dependências, logs diários e streak de sobriedade")
@SecurityRequirement(name = "bearerAuth")
public class AddictionController {

    private final AddictionService addictionService;
    private final JwtService jwtService;

    public AddictionController(AddictionService addictionService, JwtService jwtService) {
        this.addictionService = addictionService;
        this.jwtService = jwtService;
    }

    @PostMapping("/users/{userId}")
    @Operation(summary = "Cria uma dependência para o usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dependência criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<?> createAddiction(@PathVariable UUID userId,
                                             @RequestBody @Valid CreateAddictionRequest request) {
        return ResultHttpResponseHelper.respond(addictionService.createAddiction(userId, request), HttpStatus.CREATED);
    }

    @PutMapping("/{addictionId}/logs/users/{userId}")
    @Operation(summary = "Registra ou atualiza o log diário de uma dependência")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Log registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Dependência não encontrada")
    })
    public ResponseEntity<?> registerAddictionLog(@PathVariable UUID addictionId,
                                                  @PathVariable UUID userId,
                                                  @RequestBody @Valid RegisterAddictionLogRequest request) {
        return ResultHttpResponseHelper.respond(addictionService.registerAddictionLog(addictionId, userId, request), HttpStatus.OK);
    }

    @GetMapping("/{addictionId}/logs/users/{userId}")
    @Operation(summary = "Lista logs de dependência com filtro opcional por período")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Intervalo de datas inválido"),
            @ApiResponse(responseCode = "404", description = "Dependência não encontrada")
    })
    public ResponseEntity<?> getAddictionLogs(@PathVariable UUID addictionId,
                                              @PathVariable UUID userId,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResultHttpResponseHelper.respond(addictionService.getAddictionLogs(addictionId, userId, fromDate, toDate), HttpStatus.OK);
    }

    @GetMapping("/{addictionId}/sobriety/users/{userId}")
    @Operation(summary = "Calcula a streak atual de sobriedade")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sobriedade calculada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Dependência não encontrada")
    })
    public ResponseEntity<?> getCurrentSobrietyStreak(@PathVariable UUID addictionId,
                                                      @PathVariable UUID userId) {
        return ResultHttpResponseHelper.respond(addictionService.getCurrentSobrietyStreak(addictionId, userId), HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "Lista dependências do usuário autenticado (paginação)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<?> findAllForAuthenticated(@RequestHeader(name = "Authorization", required = false) String authorization,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorization.substring("Bearer ".length());
        String email = jwtService.extractEmail(token);
        return ResultHttpResponseHelper.respond(addictionService.findAllByEmail(email, page, size), HttpStatus.OK);
    }
}

