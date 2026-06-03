package com.diegoramos.mylifediary.modules.addiction.dto.response;

import java.util.UUID;

/**
 * Representação da métrica principal do módulo: dias consecutivos sem recaída.
 */
public record AddictionSobrietyStreakResponseDTO(
        UUID addictionId,
        int currentSobrietyStreak
) {
}

