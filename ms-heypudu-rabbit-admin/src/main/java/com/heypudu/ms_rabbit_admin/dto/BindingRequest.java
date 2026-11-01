package com.heypudu.ms_rabbit_admin.dto;

import jakarta.validation.constraints.NotBlank;

public record BindingRequest(
        @NotBlank(message = "El exchange es obligatorio")
        String exchange,
        @NotBlank(message = "La cola es obligatoria")
        String queue,
        @NotBlank(message = "La routing key es obligatoria")
        String routingKey
) {
}
