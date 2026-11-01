package com.heypudu.ms_rabbit_admin.dto;

import jakarta.validation.constraints.NotBlank;

public record ExchangeRequest(
        @NotBlank(message = "El tipo de exchange es obligatorio")
        String type
) {
}
