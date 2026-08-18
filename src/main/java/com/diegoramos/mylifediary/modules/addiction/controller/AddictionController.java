package com.diegoramos.mylifediary.modules.addiction.controller;

import com.diegoramos.mylifediary.common.response.ResultHttpResponseHelper;
import com.diegoramos.mylifediary.config.security.CustomUserDetails;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    public AddictionController(AddictionService addictionService) {
        this.addictionService = addictionService;
    }

    @PostMapping
    @Operation(summary = "Cria uma dependência para o usuário autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dependência criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<?> createAddiction(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody @Valid CreateAddictionRequest request) {

        UUID userId = currentUser.getId();

        return ResultHttpResponseHelper.respond(
                addictionService.createAddiction(userId, request),
                HttpStatus.CREATED
        );
    }

    @DeleteMapping("/{addictionId}")
    @Operation(summary = "Exclui uma dependência do usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Dependência excluída com sucesso"),
            @ApiResponse(responseCode = "403", description = "Usuário não autorizado"),
            @ApiResponse(responseCode = "404", description = "Dependência não encontrada")
    })
    public ResponseEntity<?> deleteAddiction(@PathVariable UUID addictionId,
                                             @AuthenticationPrincipal CustomUserDetails currentUser) {
        UUID userId = currentUser.getId();
        return ResultHttpResponseHelper.respond(
                addictionService.deleteAddiction(addictionId, userId),
                HttpStatus.NO_CONTENT
        );
    }

    @PutMapping("/{addictionId}/logs")
    @Operation(summary = "Registra ou atualiza o log diário de uma dependência")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Log registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Dependência não encontrada")
    })
    public ResponseEntity<?> registerAddictionLog(
            @PathVariable UUID addictionId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody @Valid RegisterAddictionLogRequest request) {

        UUID userId = currentUser.getId();

        return ResultHttpResponseHelper.respond(
                addictionService.registerAddictionLog(
                        addictionId,
                        userId,
                        request
                ),
                HttpStatus.OK
        );
    }

    @GetMapping("/{addictionId}/logs")
    @Operation(summary = "Lista logs de dependência com filtro opcional por período")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Intervalo de datas inválido"),
            @ApiResponse(responseCode = "404", description = "Dependência não encontrada")
    })
    public ResponseEntity<?> getAddictionLogs(@PathVariable UUID addictionId,
                                              @AuthenticationPrincipal CustomUserDetails currentUser,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        UUID userId = currentUser.getId();

        return ResultHttpResponseHelper.respond(addictionService.getAddictionLogs(addictionId, userId, fromDate, toDate), HttpStatus.OK);
    }

    @GetMapping("/{addictionId}/sobriety")
    @Operation(summary = "Calcula a streak atual de sobriedade")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sobriedade calculada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Dependência não encontrada")
    })
    public ResponseEntity<?> getCurrentSobrietyStreak(@PathVariable UUID addictionId,
                                                      @AuthenticationPrincipal CustomUserDetails currentUser) {

        UUID userId = currentUser.getId();

        return ResultHttpResponseHelper.respond(addictionService.getCurrentSobrietyStreak(addictionId, userId), HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "Lista dependências do usuário autenticado (paginação)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<?> findAllForAuthenticated(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = currentUser.getId();

        return ResultHttpResponseHelper.respond(
                addictionService.findAll(userId, page, size),
                HttpStatus.OK
        );
    }
}

