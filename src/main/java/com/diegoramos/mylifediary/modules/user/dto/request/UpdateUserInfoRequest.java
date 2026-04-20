package com.diegoramos.mylifediary.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateUserInfoRequest(
        @NotBlank(message = "O nome a ser atualizado não pode estar vazio")
        String newFullName,

        @NotNull(message = "A data de nascimento a ser atualizada não pode estar vazio")
        LocalDate newDateBirth
) {
}
