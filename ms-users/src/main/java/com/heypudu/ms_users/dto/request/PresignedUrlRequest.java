package com.heypudu.ms_users.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PresignedUrlRequest(

        @NotBlank(message = "El nombre del archivo es obligatorio")
        String fileName,

        @NotBlank(message = "El content type es obligatorio")
        @Pattern(
                regexp = "image/.*",
                message = "El content type debe ser image/*"
        )
        String contentType
) {
}