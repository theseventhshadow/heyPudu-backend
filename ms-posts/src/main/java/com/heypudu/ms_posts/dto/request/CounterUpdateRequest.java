package com.heypudu.ms_posts.dto.request;

import jakarta.validation.constraints.NotNull;

public record CounterUpdateRequest(

        @NotNull(message = "El delta de likes es obligatorio")
        Integer likeDelta,

        @NotNull(message = "El delta de comentarios es obligatorio")
        Integer commentDelta
) {
}