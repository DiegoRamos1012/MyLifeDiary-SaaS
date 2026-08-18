package com.diegoramos.mylifediary.modules.journal.controller;

import com.diegoramos.mylifediary.common.response.ResultHttpResponseHelper;
import com.diegoramos.mylifediary.config.security.CustomUserDetails;
import com.diegoramos.mylifediary.modules.journal.dto.request.*;
import com.diegoramos.mylifediary.modules.journal.service.JournalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/journals")
@Tag(
        name = "Journals",
        description = "Operações de diários, bloqueio por senha e notas diárias"
)
@SecurityRequirement(name = "bearerAuth")
public class JournalController {

    private static final String JOURNAL_PASSWORD_HEADER = "X-Journal-Password";

    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    @PostMapping
    @Operation(summary = "Cria um diário para o usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Diário criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<?> createJournal(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody @Valid CreateJournalRequest request
    ) {

        UUID userId = currentUser.getId();

        return ResultHttpResponseHelper.respond(
                journalService.createJournal(userId, request),
                HttpStatus.CREATED
        );
    }

    @PatchMapping("{journalId}/lock")
    @Operation(summary = "Tranca um diário com senha")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Diário trancado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Senha inválida ou diário já trancado"),
            @ApiResponse(responseCode = "404", description = "Diário não encontrado")
    })
    public ResponseEntity<?> lockJournal(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID journalId,
            @RequestBody @Valid LockJournalRequest request
    ) {

        UUID userId = currentUser.getId();

        return ResultHttpResponseHelper.respond(
                journalService.lockJournal(userId, journalId, request),
                HttpStatus.OK
        );
    }

    @PatchMapping("{journalId}/unlock")
    @Operation(summary = "Destranca um diário com senha")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Diário destrancado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Senha inválida ou diário já destrancado"),
            @ApiResponse(responseCode = "404", description = "Diário não encontrado")
    })
    public ResponseEntity<?> unlockJournal(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID journalId,
            @RequestBody @Valid UnlockJournalRequest request
    ) {

        UUID userId = currentUser.getId();

        return ResultHttpResponseHelper.respond(
                journalService.unlockJournal(userId, journalId, request),
                HttpStatus.OK
        );
    }

    @PostMapping("{journalId}/entries")
    @Operation(
            summary = "Cria uma nota diária no diário",
            description = """
                    Se o diário estiver trancado, informe a senha no header
                    X-Journal-Password. A senha apenas autoriza a operação;
                    o diário permanece trancado.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nota criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos, senha ausente/incorreta ou data duplicada"),
            @ApiResponse(responseCode = "404", description = "Diário não encontrado")
    })
    public ResponseEntity<?> createEntry(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID journalId,
            @RequestHeader(
                    value = JOURNAL_PASSWORD_HEADER,
                    required = false
            ) String password,
            @RequestBody @Valid CreateJournalEntryRequest request
    ) {

        UUID userId = currentUser.getId();

        return ResultHttpResponseHelper.respond(
                journalService.createEntry(
                        userId,
                        journalId,
                        request,
                        password
                ),
                HttpStatus.CREATED
        );
    }

    @PutMapping("{journalId}/entries/{entryId}")
    @Operation(
            summary = "Atualiza uma nota diária",
            description = """
                    Se o diário estiver trancado, informe a senha no header
                    X-Journal-Password. A senha apenas autoriza a operação;
                    o diário permanece trancado.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nota atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou senha ausente/incorreta"),
            @ApiResponse(responseCode = "404", description = "Diário ou nota não encontrados")
    })
    public ResponseEntity<?> updateEntry(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID journalId,
            @PathVariable UUID entryId,
            @RequestHeader(
                    value = JOURNAL_PASSWORD_HEADER,
                    required = false
            ) String password,
            @RequestBody @Valid UpdateJournalEntryRequest request
    ) {

        UUID userId = currentUser.getId();

        return ResultHttpResponseHelper.respond(
                journalService.updateEntry(
                        userId,
                        journalId,
                        entryId,
                        request,
                        password
                ),
                HttpStatus.OK
        );
    }

    @DeleteMapping("{journalId}/entries/{entryId}")
    @Operation(
            summary = "Remove uma nota diária",
            description = """
                    Se o diário estiver trancado, informe a senha no header
                    X-Journal-Password. A senha apenas autoriza a operação;
                    o diário permanece trancado.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Nota removida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Senha ausente ou incorreta"),
            @ApiResponse(responseCode = "404", description = "Diário ou nota não encontrados")
    })
    public ResponseEntity<?> deleteEntry(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID journalId,
            @PathVariable UUID entryId,
            @RequestHeader(
                    value = JOURNAL_PASSWORD_HEADER,
                    required = false
            ) String password
    ) {

        UUID userId = currentUser.getId();

        return ResultHttpResponseHelper.respond(
                journalService.deleteEntry(
                        userId,
                        journalId,
                        entryId,
                        password
                ),
                HttpStatus.NO_CONTENT
        );
    }

    @GetMapping("/{journalId}/entries/{entryId}")
    @Operation(
            summary = "Busca uma nota diária por id",
            description = """
                    Se o diário estiver trancado, informe a senha no header
                    X-Journal-Password.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nota encontrada"),
            @ApiResponse(responseCode = "400", description = "Senha ausente ou incorreta"),
            @ApiResponse(responseCode = "404", description = "Diário ou nota não encontrados")
    })
    public ResponseEntity<?> findEntry(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID journalId,
            @PathVariable UUID entryId,
            @RequestHeader(
                    value = JOURNAL_PASSWORD_HEADER,
                    required = false
            ) String password
    ) {

        UUID userId = currentUser.getId();

        return ResultHttpResponseHelper.respond(
                journalService.findEntry(
                        userId,
                        journalId,
                        entryId,
                        password
                ),
                HttpStatus.OK
        );
    }

    @GetMapping("{journalId}/entries")
    @Operation(
            summary = "Lista notas diárias do diário com paginação",
            description = """
                    Se o diário estiver trancado, informe a senha no header
                    X-Journal-Password.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notas listadas com sucesso"),
            @ApiResponse(responseCode = "400", description = "Senha ausente ou incorreta"),
            @ApiResponse(responseCode = "404", description = "Diário não encontrado")
    })
    public ResponseEntity<?> listEntries(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID journalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(
                    value = JOURNAL_PASSWORD_HEADER,
                    required = false
            ) String password
    ) {

        UUID userId = currentUser.getId();
        
        return ResultHttpResponseHelper.respond(
                journalService.listEntries(
                        userId,
                        journalId,
                        page,
                        size,
                        password
                ),
                HttpStatus.OK
        );
    }
}
