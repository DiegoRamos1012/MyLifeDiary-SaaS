package com.diegoramos.mylifediary.modules.auth.controller;

import com.diegoramos.mylifediary.common.response.ResultHttpResponseHelper;
import com.diegoramos.mylifediary.modules.auth.dto.request.LoginRequest;
import com.diegoramos.mylifediary.modules.auth.dto.request.RefreshRequest;
import com.diegoramos.mylifediary.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Autenticação e emissão de token JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica usuário e retorna token JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token gerado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Payload inválido"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas ou conta inativa")
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return ResultHttpResponseHelper.respond(authService.authenticate(request), HttpStatus.OK);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova access token e refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens renovados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Payload inválido"),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido, revogado ou expirado")
    })
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResultHttpResponseHelper.respond(authService.refresh(request), HttpStatus.OK);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoga o refresh token informado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout realizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Payload inválido"),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido ou inexistente")
    })
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshRequest request) {
        return ResultHttpResponseHelper.respond(authService.logout(request), HttpStatus.NO_CONTENT);
    }
}


