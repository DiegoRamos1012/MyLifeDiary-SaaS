package com.diegoramos.mylifediary.modules.user.dto.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO de edição parcial do perfil do usuário.
 *
 * <p>Intencionalmente os campos são opcionais: <b>null</b> significa "não alterar".
 * Este DTO NÃO impõe validações rígidas de presença (sem {@code @NotNull} ou
 * {@code @NotBlank}) para permitir que o frontend envie apenas os campos que o
 * usuário decidiu alterar na tela de edição.</p>
 *
 * <p>Comportamento adicional:
 * <ul>
 *   <li>Se {@code newFullName} for <b>null</b> → o campo não será alterado.</li>
 *   <li>Se {@code newFullName} estiver presente, o Bean Validation aplicará o
 *       {@code @Size(min = 2, max = 128)} — isso protege contra nomes muito
 *       grandes ou muito curtos quando o campo é enviado.</li>
 *   <li>Se {@code newDateBirth} for <b>null</b> → a data não será alterada.</li>
 * </ul>
 * </p>
 *
 * <p>Observações sobre responsabilidades:
 * <ul>
 *   <li>O frontend pode (e deveria) evitar enviar requisições vazias para reduzir
 *       tráfego e melhorar UX. Ex.: desabilitar o botão "Salvar" quando nada mudou.</li>
 *   <li>O backend mantém validação defensiva: o serviço deve checar se ao menos um
 *       campo foi enviado e validar semanticamente o que vier (ex.: string vazia),
 *       retornando um {@code Result.failure(...)} padronizado quando necessário.</li>
 * </ul>
 * </p>
 */
public record UpdateUserInfoRequest(
        @Size(min = 2, max = 128)
        String newFullName,

        LocalDate newDateBirth
) {
}
