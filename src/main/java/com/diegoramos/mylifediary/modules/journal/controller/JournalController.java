package com.diegoramos.mylifediary.modules.journal.controller;

import com.diegoramos.mylifediary.common.response.ResultHttpResponseHelper;
import com.diegoramos.mylifediary.modules.journal.dto.request.CreateJournalEntryRequest;
import com.diegoramos.mylifediary.modules.journal.dto.request.CreateJournalRequest;
import com.diegoramos.mylifediary.modules.journal.dto.request.LockJournalRequest;
import com.diegoramos.mylifediary.modules.journal.dto.request.UnlockJournalRequest;
import com.diegoramos.mylifediary.modules.journal.dto.request.UpdateJournalEntryRequest;
import com.diegoramos.mylifediary.modules.journal.service.JournalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/journals")
@Tag(name = "Journals", description = "Operações de diários, bloqueio por senha e notas diárias")
@SecurityRequirement(name = "bearerAuth")
public class JournalController {

    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    @PostMapping("/users/{userId}")
    @Operation(summary = "Cria um diário para o usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Diário criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<?> createJournal(@PathVariable UUID userId,
                                           @RequestBody @Valid CreateJournalRequest request) {
        return ResultHttpResponseHelper.respond(journalService.createJournal(userId, request), HttpStatus.CREATED);
    }

    @PatchMapping("/users/{userId}/{journalId}/lock")
    @Operation(summary = "Tranca um diário com senha")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Diário trancado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Senha inválida ou diário já trancado"),
            @ApiResponse(responseCode = "404", description = "Diário não encontrado")
    })
    public ResponseEntity<?> lockJournal(@PathVariable UUID userId,
                                         @PathVariable UUID journalId,
                                         @RequestBody @Valid LockJournalRequest request) {
        return ResultHttpResponseHelper.respond(journalService.lockJournal(userId, journalId, request), HttpStatus.OK);
    }

    @PatchMapping("/users/{userId}/{journalId}/unlock")
    @Operation(summary = "Destranca um diário com senha")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Diário destrancado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Senha inválida ou diário já destrancado"),
            @ApiResponse(responseCode = "404", description = "Diário não encontrado")
    })
    public ResponseEntity<?> unlockJournal(@PathVariable UUID userId,
                                           @PathVariable UUID journalId,
                                           @RequestBody @Valid UnlockJournalRequest request) {
        return ResultHttpResponseHelper.respond(journalService.unlockJournal(userId, journalId, request), HttpStatus.OK);
    }

    @PostMapping("/users/{userId}/{journalId}/entries")
    @Operation(summary = "Cria uma nota diária no diário")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nota criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos, diário trancado ou data duplicada"),
            @ApiResponse(responseCode = "404", description = "Diário não encontrado")
    })
    public ResponseEntity<?> createEntry(@PathVariable UUID userId,
                                         @PathVariable UUID journalId,
                                         @RequestBody @Valid CreateJournalEntryRequest request) {
        return ResultHttpResponseHelper.respond(journalService.createEntry(userId, journalId, request), HttpStatus.CREATED);
    }

    @PutMapping("/users/{userId}/{journalId}/entries/{entryId}")
    @Operation(summary = "Atualiza uma nota diária")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nota atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou diário trancado"),
            @ApiResponse(responseCode = "404", description = "Diário ou nota não encontrados")
    })
    public ResponseEntity<?> updateEntry(@PathVariable UUID userId,
                                         @PathVariable UUID journalId,
                                         @PathVariable UUID entryId,
                                         @RequestBody @Valid UpdateJournalEntryRequest request) {
        return ResultHttpResponseHelper.respond(journalService.updateEntry(userId, journalId, entryId, request), HttpStatus.OK);
    }

    @DeleteMapping("/users/{userId}/{journalId}/entries/{entryId}")
    @Operation(summary = "Remove uma nota diária")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Nota removida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Diário trancado"),
            @ApiResponse(responseCode = "404", description = "Diário ou nota não encontrados")
    })
    public ResponseEntity<?> deleteEntry(@PathVariable UUID userId,
                                         @PathVariable UUID journalId,
                                         @PathVariable UUID entryId) {
        return ResultHttpResponseHelper.respond(journalService.deleteEntry(userId, journalId, entryId), HttpStatus.NO_CONTENT);
    }

    @GetMapping("/users/{userId}/{journalId}/entries/{entryId}")
    @Operation(summary = "Busca uma nota diária por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nota encontrada"),
            @ApiResponse(responseCode = "404", description = "Diário ou nota não encontrados")
    })
    public ResponseEntity<?> findEntry(@PathVariable UUID userId,
                                       @PathVariable UUID journalId,
                                       @PathVariable UUID entryId) {
        return ResultHttpResponseHelper.respond(journalService.findEntry(userId, journalId, entryId), HttpStatus.OK);
    }

    @GetMapping("/users/{userId}/{journalId}/entries")
    @Operation(summary = "Lista notas diárias do diário com paginação")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notas listadas com sucesso"),
            @ApiResponse(responseCode = "404", description = "Diário não encontrado")
    })
    public ResponseEntity<?> listEntries(@PathVariable UUID userId,
                                         @PathVariable UUID journalId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return ResultHttpResponseHelper.respond(journalService.listEntries(userId, journalId, page, size), HttpStatus.OK);
    }
}

