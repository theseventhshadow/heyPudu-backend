package com.heypudu.ms_interactions.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(

        @NotBlank(message = "El contenido del comentario no puede estar vacio")
        @Size(max = 500, message = "El contenido del comentario no puede superar los 500 caracteres")
        String content
) {
}