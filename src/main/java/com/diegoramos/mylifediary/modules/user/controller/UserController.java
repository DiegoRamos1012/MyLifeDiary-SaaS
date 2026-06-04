package com.diegoramos.mylifediary.modules.user.controller;

import com.diegoramos.mylifediary.common.response.ResultHttpResponseHelper;
import com.diegoramos.mylifediary.modules.user.domain.enums.UserStatus;
import com.diegoramos.mylifediary.modules.user.dto.request.CreateUserRequest;
import com.diegoramos.mylifediary.modules.user.dto.request.UpdateEmailRequest;
import com.diegoramos.mylifediary.modules.user.dto.request.UpdatePasswordRequest;
import com.diegoramos.mylifediary.modules.user.dto.request.UpdateUserInfoRequest;
import com.diegoramos.mylifediary.modules.user.dto.response.UserResponseDTO;
import com.diegoramos.mylifediary.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Operações relacionadas a usuários")
@SecurityRequirement(name = "bearerAuth")

public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Lista todos os usuários com paginação e filtro opcional por nome")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuários listados com sucesso")
    })
    public ResponseEntity<Page<UserResponseDTO>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.findAll(search, page, size));
    }

    @PostMapping("/register")
    @Operation(summary = "Cria um novo usuário", security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Falha esperada ao criar usuário")
    })
    public ResponseEntity<?> register(@RequestBody @Valid CreateUserRequest dto) {
        return ResultHttpResponseHelper.respond(userService.register(dto), HttpStatus.CREATED);
    }

    @PatchMapping("/{userId}/userInfo")
    @Operation(summary = "Atualiza informações adicionais do usuário, como nome completo e data de nascimento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Falha ao atualizar usuário"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<?> changeProfileInfo(@PathVariable UUID userId,
                                               @RequestBody @Valid UpdateUserInfoRequest dto) {
        return ResultHttpResponseHelper.respond(userService.changeProfileInfo(userId, dto), HttpStatus.OK);
    }

    @PatchMapping("/{userId}/changeEmail")
    @Operation(summary = "Atualiza o email do usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Falha au atualizar email"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<?> changeEmail(@PathVariable UUID userId,
                                         @RequestBody @Valid UpdateEmailRequest dto) {
        return ResultHttpResponseHelper.respond(userService.changeEmail(userId, dto), HttpStatus.OK);
    }

    @PatchMapping("/{userId}/changePassword")
    @Operation(summary = "Atualiza a senha do usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Falha ao atualizar senha"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<?> changePassword(@PathVariable UUID userId,
                                            @RequestBody @Valid UpdatePasswordRequest dto) {
        return ResultHttpResponseHelper.respond(userService.changePassword(userId, dto), HttpStatus.OK);
    }

    @PatchMapping("/{userId}/deactivate")
    @Operation(summary = "Solicita a desativação (deleção lógica) do usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Solicitação de desativação recebida"),
            @ApiResponse(responseCode = "400", description = "Falha ao desativar usuário"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<?> deactivateUser(@PathVariable UUID userId) {
        return ResultHttpResponseHelper.respond(userService.deleteUser(userId), HttpStatus.ACCEPTED);
    }

    @PatchMapping("/{userId}/reactivate")
    @Operation(summary = "Reativa um usuário previamente desativado")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Solicitação de reativação recebida"),
            @ApiResponse(responseCode = "400", description = "Falha ao reativar usuário"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<?> reactivateUser(@PathVariable UUID userId) {
        return ResultHttpResponseHelper.respond(userService.restoreUser(userId), HttpStatus.ACCEPTED);
    }

    @GetMapping("/by-status")
    @Operation(summary = "Lista usuários filtrados por status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuários filtrados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Falha ao aplicar o filtro"),
            @ApiResponse(responseCode = "404", description = "Nenhum usuário encontrado")
    })
    public ResponseEntity<Page<UserResponseDTO>> findUsersByStatus(
            @Parameter(description = "Lista de status para filtrar usuários. Ex.: ACTIVE, SUSPENDED")
            @RequestParam List<UserStatus> statusList,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.findUsersByStatus(statusList, page, size));
    }
}
