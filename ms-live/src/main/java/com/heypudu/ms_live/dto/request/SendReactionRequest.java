package com.heypudu.ms_live.dto.request;

import com.heypudu.ms_live.messaging.ReactionType;
import jakarta.validation.constraints.NotNull;

public record SendReactionRequest(
        @NotNull(message = "El tipo de reacción es obligatorio") ReactionType type
) {
}
