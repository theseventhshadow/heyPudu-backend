package com.heypudu.ms_posts.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PresignedUrlRequest(

        @NotBlank(message = "El nombre del archivo es obligatorio")
        String fileName,

        @NotBlank(message = "El content type es obligatorio")
        @Pattern(
                regexp = "audio/.*|image/.*",
                message = "El content type debe ser audio/* o image/*"
        )
        String contentType
) {
}