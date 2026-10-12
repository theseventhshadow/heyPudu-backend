package com.heypudu.ms_auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateUserProfileRequest(

        @NotNull(message = "El cognitoSub es obligatorio")
        UUID cognitoSub,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        @NotBlank(message = "El username es obligatorio")
        @Size(max = 50, message = "El username no puede superar los 50 caracteres")
        @Pattern(
                regexp = "^[a-zA-Z0-9._]+$",
                message = "El username solo puede contener letras, numeros, puntos y guiones bajos"
        )
        String username,

        @Size(max = 300, message = "La bio no puede superar los 300 caracteres")
        String bio,

        @Size(max = 500, message = "La clave del avatar no puede superar los 500 caracteres")
        String avatarKey
) {
}