package com.diegoramos.mylifediary.modules.user.controller;

import com.diegoramos.mylifediary.common.response.ResultHttpResponseHelper;
import com.diegoramos.mylifediary.modules.user.dto.request.CreateUserRequest;
import com.diegoramos.mylifediary.modules.user.dto.request.UpdatePasswordRequest;
import com.diegoramos.mylifediary.modules.user.dto.request.UpdateUserInfoRequest;
import com.diegoramos.mylifediary.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Operações relacionadas a usuários")
// @SecurityRequirement(name = bearerAuth)

public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "Cria um novo usuário")
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

    @PatchMapping("/{userId}")
    @Operation(summary = "Atualiza o email do usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Falha au atualizar email"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<?> updateEmail(@PathVariable UUID userId,
                                         @RequestBody @Valid UpdatePasswordRequest dto) {
        return ResultHttpResponseHelper.respond(userService.changePassword(userId, dto), HttpStatus.OK);
    }

}

