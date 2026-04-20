package com.diegoramos.mylifediary.modules.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateEmailRequest(
        @NotBlank(message = "O e-mail a ser atualizado não pode estar vazio")
        @Email
        String newEmail
) {
}
