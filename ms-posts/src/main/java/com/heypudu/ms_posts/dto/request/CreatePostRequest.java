package com.heypudu.ms_posts.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(

        @NotBlank(message = "El tipo de publicacion es obligatorio")
        @Pattern(regexp = "TEXT|AUDIO", message = "El tipo debe ser TEXT o AUDIO")
        String type,

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 50, message = "El titulo no puede superar los 50 caracteres")
        String title,

        @Size(max = 5000, message = "El contenido no puede superar los 5000 caracteres")
        String content,

        @Size(max = 500, message = "La clave del audio no puede superar los 500 caracteres")
        String audioKey,

        @Min(value = 1, message = "La duracion del audio debe ser al menos 1 segundo")
        @Max(value = 300, message = "La duracion del audio no puede superar los 300 segundos")
        Integer audioDurationSeconds,

        @Size(max = 500, message = "La clave de la portada no puede superar los 500 caracteres")
        String coverImageKey
) {
}