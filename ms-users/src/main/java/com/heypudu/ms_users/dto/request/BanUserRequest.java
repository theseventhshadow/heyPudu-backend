package com.heypudu.ms_users.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BanUserRequest(

        @NotBlank(message = "El motivo del baneo es obligatorio")
        @Size(max = 300, message = "El motivo no puede superar los 300 caracteres")
        String reason
) {
}