package com.diegoramos.mylifediary.modules.user.dto.request;

import java.time.LocalDate;

public record UpdateUserInfoRequest(
        String newFullName,

        LocalDate newDateBirth
) {
}
