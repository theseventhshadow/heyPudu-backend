package com.heypudu.ms_posts.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 50, message = "El titulo no puede superar los 50 caracteres")
        String title,

        @Size(max = 5000, message = "El contenido no puede superar los 5000 caracteres")
        String content,

        @Size(max = 500, message = "La clave de la portada no puede superar los 500 caracteres")
        String coverImageKey
) {
}