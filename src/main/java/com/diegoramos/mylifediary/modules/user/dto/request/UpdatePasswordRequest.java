package com.diegoramos.mylifediary.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
        @NotBlank(message = "A senha a ser atualizada não pode estar vazia")
        @Size(min = 8, max = 128)
        String newPassword
) {
}
